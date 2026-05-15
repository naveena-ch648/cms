package com.cms.service;

import com.cms.entity.User;
import com.cms.entity.UserTwoFactor;
import com.cms.entity.UserTwoFactor.TwoFactorMethod;
import com.cms.repository.UserTwoFactorRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TwoFactorService {

    private static final String EMAIL_OTP_PREFIX = "2fa:email:";
    private static final int BACKUP_CODE_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 10;

    private final UserTwoFactorRepository twoFactorRepository;
    private final JdbcTemplate pgJdbc;
    private final JavaMailSenderWrapper mailWrapper;

    public TwoFactorService(UserTwoFactorRepository twoFactorRepository,
                            @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc,
                            JavaMailSenderWrapper mailWrapper) {
        this.twoFactorRepository = twoFactorRepository;
        this.pgJdbc = pgJdbc;
        this.mailWrapper = mailWrapper;
    }

    @Value("${two-factor.issuer}")
    private String issuer;

    @Value("${two-factor.email-otp-ttl-seconds:300}")
    private long emailOtpTtlSeconds;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // ─── Setup ────────────────────────────────────────────────────────────────

    /**
     * Generates a new TOTP secret and returns the otpauth:// URI for QR display.
     * Does NOT enable 2FA yet — call {@link #confirmTotpSetup} after the user
     * verifies the first code.
     */
    @Transactional
    public String initiateTotpSetup(User user) {
        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        String secret = credentials.getKey();

        UserTwoFactor tf = getOrCreate(user);
        tf.setMethod(TwoFactorMethod.TOTP);
        tf.setTotpSecret(secret);
        tf.setEnabled(false);
        twoFactorRepository.save(tf);

        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, user.getEmail(), credentials);
    }

    /**
     * Confirms TOTP setup by verifying the first code from the user's authenticator app.
     * On success, enables 2FA and returns fresh backup codes.
     */
    @Transactional
    public List<String> confirmTotpSetup(User user, int code) {
        UserTwoFactor tf = twoFactorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("No 2FA setup in progress for user"));

        if (tf.getTotpSecret() == null) {
            throw new IllegalStateException("TOTP secret not initialised");
        }
        if (!gAuth.authorize(tf.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Invalid TOTP code — please try again");
        }

        List<String> backupCodes = generateBackupCodes();
        tf.setEnabled(true);
        tf.setBackupCodes(String.join(",", backupCodes));
        twoFactorRepository.save(tf);

        return backupCodes;
    }

    /**
     * Configures email-based OTP as the 2FA method and enables it immediately.
     */
    @Transactional
    public List<String> enableEmailOtp(User user) {
        UserTwoFactor tf = getOrCreate(user);
        tf.setMethod(TwoFactorMethod.EMAIL);
        tf.setTotpSecret(null);
        tf.setEnabled(true);
        List<String> backupCodes = generateBackupCodes();
        tf.setBackupCodes(String.join(",", backupCodes));
        twoFactorRepository.save(tf);
        return backupCodes;
    }

    @Transactional
    public void disable(User user) {
        twoFactorRepository.findByUserId(user.getId()).ifPresent(tf -> {
            tf.setEnabled(false);
            tf.setTotpSecret(null);
            tf.setBackupCodes(null);
            twoFactorRepository.save(tf);
        });
    }

    // ─── Verification ─────────────────────────────────────────────────────────

    public boolean isEnabled(Long userId) {
        return twoFactorRepository.findByUserId(userId)
                .map(UserTwoFactor::isEnabled)
                .orElse(false);
    }

    /**
     * Sends a one-time code to the user's email address (used for EMAIL method).
     */
    public void sendEmailOtp(User user) {
        String otp = generateNumericOtp(6);
        String key = EMAIL_OTP_PREFIX + user.getId();
        pgJdbc.update("""
                INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                VALUES (?, 'EMAIL_OTP', ?, NOW() + (? * INTERVAL '1 second'))
                ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                """, key, otp, emailOtpTtlSeconds);

        try {
            mailWrapper.sendSimple(
                    user.getEmail(),
                    "Your CMS verification code",
                    "Your verification code is: " + otp + "\n\nIt expires in " +
                            (emailOtpTtlSeconds / 60) + " minutes."
            );
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    /**
     * Verifies a code submitted by the user during login.
     * Supports TOTP codes, email OTPs, and backup codes.
     */
    @Transactional
    public boolean verifyCode(User user, String code) {
        UserTwoFactor tf = twoFactorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("2FA not configured"));

        if (!tf.isEnabled()) return true;

        // Backup code check (always available regardless of method)
        if (isBackupCode(tf, code)) return true;

        return switch (tf.getMethod()) {
            case TOTP -> {
                try {
                    yield gAuth.authorize(tf.getTotpSecret(), Integer.parseInt(code.replaceAll("\\s", "")));
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case EMAIL -> {
                String key = EMAIL_OTP_PREFIX + user.getId();
                String stored = null;
                try {
                    stored = pgJdbc.queryForObject(
                        "SELECT value FROM jwt_tokens WHERE jti=? AND token_type='EMAIL_OTP' AND expires_at>NOW()",
                        String.class, key);
                } catch (Exception ignored) {}
                if (stored != null && stored.equals(code.trim())) {
                    pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", key);
                    yield true;
                }
                yield false;
            }
        };
    }

    public Optional<TwoFactorMethod> getMethod(Long userId) {
        return twoFactorRepository.findByUserId(userId)
                .filter(UserTwoFactor::isEnabled)
                .map(UserTwoFactor::getMethod);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UserTwoFactor getOrCreate(User user) {
        return twoFactorRepository.findByUserId(user.getId())
                .orElseGet(() -> UserTwoFactor.builder().user(user).build());
    }

    private boolean isBackupCode(UserTwoFactor tf, String code) {
        if (tf.getBackupCodes() == null || tf.getBackupCodes().isBlank()) return false;
        List<String> codes = new ArrayList<>(List.of(tf.getBackupCodes().split(",")));
        if (codes.remove(code.trim())) {
            tf.setBackupCodes(String.join(",", codes));
            twoFactorRepository.save(tf);
            return true;
        }
        return false;
    }

    private List<String> generateBackupCodes() {
        SecureRandom rng = new SecureRandom();
        List<String> codes = new ArrayList<>();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                sb.append(chars.charAt(rng.nextInt(chars.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    private String generateNumericOtp(int digits) {
        SecureRandom rng = new SecureRandom();
        int max = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", rng.nextInt(max));
    }
}
