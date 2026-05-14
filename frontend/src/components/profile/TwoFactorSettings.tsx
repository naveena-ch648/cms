import { useEffect, useState } from 'react';
import { twoFactorApi, type TwoFactorStatus } from '../../api/twoFactor';

export default function TwoFactorSettings() {
  const [status, setStatus] = useState<TwoFactorStatus | null>(null);
  const [step, setStep] = useState<'idle' | 'totp-qr' | 'totp-confirm' | 'backup'>('idle');
  const [otpauthUri, setOtpauthUri] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [backupCodes, setBackupCodes] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    twoFactorApi.getStatus().then((r) => setStatus(r.data.data)).catch(() => {});
  }, []);

  async function startTotpSetup() {
    setError(null);
    setLoading(true);
    try {
      const res = await twoFactorApi.initTotpSetup();
      setOtpauthUri(res.data.data.otpauthUri);
      setStep('totp-qr');
    } catch {
      setError('Failed to start TOTP setup');
    } finally {
      setLoading(false);
    }
  }

  async function confirmTotp() {
    const code = parseInt(totpCode.replace(/\s/g, ''), 10);
    if (isNaN(code)) { setError('Enter a valid 6-digit code'); return; }
    setError(null);
    setLoading(true);
    try {
      const res = await twoFactorApi.confirmTotpSetup(code);
      setBackupCodes(res.data.data.backupCodes);
      setStatus({ enabled: true, method: 'TOTP' });
      setStep('backup');
    } catch {
      setError('Invalid code — please try again');
    } finally {
      setLoading(false);
    }
  }

  async function enableEmail() {
    setError(null);
    setLoading(true);
    try {
      const res = await twoFactorApi.enableEmailOtp();
      setBackupCodes(res.data.data.backupCodes);
      setStatus({ enabled: true, method: 'EMAIL' });
      setStep('backup');
    } catch {
      setError('Failed to enable email OTP');
    } finally {
      setLoading(false);
    }
  }

  async function disable2FA() {
    if (!confirm('Are you sure you want to disable two-factor authentication?')) return;
    setError(null);
    setLoading(true);
    try {
      await twoFactorApi.disable();
      setStatus({ enabled: false, method: 'NONE' });
      setStep('idle');
    } catch {
      setError('Failed to disable 2FA');
    } finally {
      setLoading(false);
    }
  }

  if (!status) return <div style={{ padding: 24, color: '#5f6368' }}>Loading…</div>;

  return (
    <div style={{ maxWidth: 520, padding: 24 }}>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>Two-Factor Authentication</h2>
      <p style={{ color: '#5f6368', fontSize: 14, marginBottom: 20 }}>
        Add an extra layer of security to your account.
      </p>

      {/* Status banner */}
      {status.enabled && step !== 'backup' && (
        <div style={{ background: '#e6f4ea', borderRadius: 6, padding: '10px 14px', marginBottom: 20, fontSize: 14 }}>
          2FA is <strong>enabled</strong> ({status.method})
          <button onClick={disable2FA} disabled={loading}
            style={{ marginLeft: 16, color: '#c5221f', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }}>
            Disable
          </button>
        </div>
      )}

      {/* Idle / choose method */}
      {step === 'idle' && !status.enabled && (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <button onClick={startTotpSetup} disabled={loading} style={methodBtn}>
            Authenticator App (TOTP)
          </button>
          <button onClick={enableEmail} disabled={loading} style={{ ...methodBtn, background: '#f8f9fa', color: '#1a73e8' }}>
            Email OTP
          </button>
        </div>
      )}

      {/* QR code step */}
      {step === 'totp-qr' && (
        <div>
          <p style={{ fontSize: 14, marginBottom: 12 }}>
            Scan this QR code with your authenticator app, then enter the 6-digit code below.
          </p>
          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(otpauthUri)}`}
            alt="TOTP QR code"
            style={{ display: 'block', margin: '0 auto 16px', borderRadius: 8, border: '1px solid #dadce0' }}
          />
          <input
            type="text"
            inputMode="numeric"
            maxLength={7}
            placeholder="6-digit code"
            value={totpCode}
            onChange={(e) => setTotpCode(e.target.value)}
            style={inputStyle}
          />
          <button onClick={confirmTotp} disabled={loading} style={primaryBtn}>
            {loading ? 'Verifying…' : 'Confirm setup'}
          </button>
        </div>
      )}

      {/* Backup codes */}
      {step === 'backup' && (
        <div>
          <p style={{ fontSize: 14, marginBottom: 12, fontWeight: 600 }}>
            Save these backup codes — they can each be used once if you lose access:
          </p>
          <div style={{ background: '#f8f9fa', borderRadius: 6, padding: 16, fontFamily: 'monospace', fontSize: 13 }}>
            {backupCodes.map((c) => <div key={c}>{c}</div>)}
          </div>
          <button onClick={() => setStep('idle')} style={{ ...primaryBtn, marginTop: 16 }}>
            Done
          </button>
        </div>
      )}

      {error && <p style={{ color: '#ea4335', fontSize: 13, marginTop: 12 }}>{error}</p>}
    </div>
  );
}

const methodBtn: React.CSSProperties = {
  padding: '10px 20px',
  background: '#1a73e8',
  color: '#fff',
  border: 'none',
  borderRadius: 6,
  fontWeight: 600,
  fontSize: 14,
  cursor: 'pointer',
};

const primaryBtn: React.CSSProperties = {
  display: 'block',
  marginTop: 12,
  padding: '10px 24px',
  background: '#1a73e8',
  color: '#fff',
  border: 'none',
  borderRadius: 6,
  fontWeight: 600,
  fontSize: 14,
  cursor: 'pointer',
};

const inputStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  padding: '10px 12px',
  fontSize: 16,
  border: '1px solid #dadce0',
  borderRadius: 6,
  marginBottom: 10,
  letterSpacing: 4,
};
