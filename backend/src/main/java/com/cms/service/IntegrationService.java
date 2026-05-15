package com.cms.service;

import com.cms.dto.integration.*;
import com.cms.entity.*;
import com.cms.middleware.TenantContext;
import com.cms.repository.*;
import com.cms.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class IntegrationService {

    private final IntegrationConnectionRepository connectionRepository;
    private final FolderRepository folderRepository;
    private final IntegrationTokenEncryptor tokenEncryptor;
    private final JobQueueService jobQueueService;
    private final JdbcTemplate pgJdbc;
    private final ObjectMapper objectMapper;

    public IntegrationService(IntegrationConnectionRepository connectionRepository,
                              FolderRepository folderRepository,
                              IntegrationTokenEncryptor tokenEncryptor,
                              JobQueueService jobQueueService,
                              @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc,
                              ObjectMapper objectMapper) {
        this.connectionRepository = connectionRepository;
        this.folderRepository = folderRepository;
        this.tokenEncryptor = tokenEncryptor;
        this.jobQueueService = jobQueueService;
        this.pgJdbc = pgJdbc;
        this.objectMapper = objectMapper;
    }

    @Value("${google.drive.client-id}")
    private String googleClientId;

    @Value("${google.drive.client-secret}")
    private String googleClientSecret;

    @Value("${google.drive.redirect-uri}")
    private String googleRedirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/drive.file";
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";

    public String generateAuthorizationUrl(UserPrincipal user) {
        String state = UUID.randomUUID().toString();
        // Store state in PostgreSQL with 10-minute TTL for CSRF protection
        String stateKey = OAUTH_STATE_PREFIX + state;
        String stateValue = user.getId() + ":" + TenantContext.getCurrentTenant();
        pgJdbc.update("""
                INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                VALUES (?, 'OAUTH_STATE', ?, NOW() + INTERVAL '10 minutes')
                ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                """, stateKey, stateValue);

        return GOOGLE_AUTH_URL +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=" + GOOGLE_DRIVE_SCOPE.replace(" ", "%20") +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + state;
    }

    @Transactional
    public ConnectionResponse handleOAuthCallback(String code, String state, UserPrincipal user) {
        // Validate state parameter
        String stateKey = OAUTH_STATE_PREFIX + state;
        String stateValue = null;
        try {
            stateValue = pgJdbc.queryForObject(
                "SELECT value FROM jwt_tokens WHERE jti=? AND token_type='OAUTH_STATE' AND expires_at>NOW()",
                String.class, stateKey);
        } catch (Exception ignored) {}
        if (stateValue == null) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }
        pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", stateKey);

        // Exchange code for tokens (simulated — in production would call Google Token endpoint)
        Map<String, String> tokens = exchangeCodeForTokens(code);
        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.get("refresh_token");
        String email = tokens.getOrDefault("email", "connected@google.com");

        Long orgId = TenantContext.getCurrentTenant();

        // Check for existing connection
        Optional<IntegrationConnection> existing = connectionRepository
                .findByOrganizationIdAndUserIdAndProvider(orgId, user.getId(), "GOOGLE_DRIVE");
        if (existing.isPresent() && existing.get().getStatus() == IntegrationConnection.Status.ACTIVE) {
            throw new IllegalStateException("Google Drive connection already exists");
        }

        IntegrationConnection connection;
        if (existing.isPresent()) {
            connection = existing.get();
            connection.setStatus(IntegrationConnection.Status.ACTIVE);
            connection.setAccessTokenEncrypted(tokenEncryptor.encrypt(accessToken));
            connection.setRefreshTokenEncrypted(tokenEncryptor.encrypt(refreshToken));
            connection.setTokenExpiresAt(Instant.now().plusSeconds(3600));
            connection.setConnectedAt(Instant.now());
            connection.setProviderAccountId(email);
        } else {
            Organization org = new Organization();
            org.setId(orgId);
            User userEntity = new User();
            userEntity.setId(user.getId());

            connection = IntegrationConnection.builder()
                    .uuid(UUID.randomUUID().toString())
                    .organization(org)
                    .user(userEntity)
                    .provider("GOOGLE_DRIVE")
                    .providerAccountId(email)
                    .accessTokenEncrypted(tokenEncryptor.encrypt(accessToken))
                    .refreshTokenEncrypted(tokenEncryptor.encrypt(refreshToken))
                    .tokenExpiresAt(Instant.now().plusSeconds(3600))
                    .scopes(GOOGLE_DRIVE_SCOPE)
                    .status(IntegrationConnection.Status.ACTIVE)
                    .connectedAt(Instant.now())
                    .build();
        }

        connection = connectionRepository.save(connection);

        return ConnectionResponse.builder()
                .id(connection.getUuid())
                .provider(connection.getProvider())
                .providerAccountId(connection.getProviderAccountId())
                .status(connection.getStatus().name())
                .connectedAt(connection.getConnectedAt())
                .build();
    }

    public List<ConnectionResponse> getUserConnections(UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();
        List<IntegrationConnection> connections = connectionRepository
                .findByOrganizationIdAndUserId(orgId, user.getId());

        return connections.stream().map(c -> ConnectionResponse.builder()
                .id(c.getUuid())
                .provider(c.getProvider())
                .providerAccountId(c.getProviderAccountId())
                .status(c.getStatus().name())
                .connectedAt(c.getConnectedAt())
                .lastUsedAt(c.getLastUsedAt())
                .build()).toList();
    }

    @Transactional
    public void disconnectConnection(String connectionId, UserPrincipal user) {
        IntegrationConnection connection = connectionRepository.findByUuid(connectionId)
                .orElseThrow(() -> new NoSuchElementException("Connection not found"));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to disconnect this connection");
        }

        connection.setStatus(IntegrationConnection.Status.REVOKED);
        connectionRepository.save(connection);
    }

    public DriveBrowseResponse browseDrive(String folderId, String query, String pageToken, UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();
        IntegrationConnection connection = connectionRepository
                .findByOrganizationIdAndUserIdAndProvider(orgId, user.getId(), "GOOGLE_DRIVE")
                .orElseThrow(() -> new NoSuchElementException("No active Google Drive connection"));

        if (connection.getStatus() != IntegrationConnection.Status.ACTIVE) {
            throw new IllegalStateException("Google Drive connection is not active");
        }

        // Update last used timestamp
        connection.setLastUsedAt(Instant.now());
        connectionRepository.save(connection);

        // Call Google Drive API to list files (simulated for now)
        return fetchDriveFiles(connection, folderId, query, pageToken);
    }

    @Transactional
    public JobResponse importFromDrive(ImportRequest request, UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();
        IntegrationConnection connection = connectionRepository.findByUuid(request.getConnectionId())
                .orElseThrow(() -> new NoSuchElementException("Connection not found"));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to use this connection");
        }

        // Verify target folder exists
        Folder targetFolder = folderRepository.findByUuid(request.getTargetFolderId())
                .orElseThrow(() -> new NoSuchElementException("Target folder not found"));

        // Create import job and push to Redis queue
        String jobId = UUID.randomUUID().toString();
        try {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("jobId", jobId);
            jobData.put("type", "IMPORT");
            jobData.put("connectionId", connection.getId());
            jobData.put("organizationId", orgId);
            jobData.put("userId", user.getId());
            jobData.put("driveFileIds", request.getDriveFileIds());
            jobData.put("targetFolderId", targetFolder.getId());
            jobData.put("preserveStructure", request.isPreserveStructure());
            jobData.put("accessToken", tokenEncryptor.decrypt(connection.getAccessTokenEncrypted()));
            jobData.put("refreshToken", tokenEncryptor.decrypt(connection.getRefreshTokenEncrypted()));

            String json = objectMapper.writeValueAsString(jobData);
            jobQueueService.push("integration:import", json);

            // Store job status in PostgreSQL
            upsertJobState(jobId, "status", "QUEUED");
            upsertJobState(jobId, "totalItems", String.valueOf(request.getDriveFileIds().size()));
            upsertJobState(jobId, "completedItems", "0");
            upsertJobState(jobId, "failedItems", "0");
            upsertJobState(jobId, "startedAt", Instant.now().toString());

        } catch (Exception e) {
            throw new RuntimeException("Failed to queue import job", e);
        }

        return JobResponse.builder()
                .jobId(jobId)
                .status("QUEUED")
                .totalItems(request.getDriveFileIds().size())
                .message("Import job queued")
                .build();
    }

    @Transactional
    public JobResponse exportToDrive(ExportRequest request, UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();
        IntegrationConnection connection = connectionRepository.findByUuid(request.getConnectionId())
                .orElseThrow(() -> new NoSuchElementException("Connection not found"));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to use this connection");
        }

        String jobId = UUID.randomUUID().toString();
        try {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("jobId", jobId);
            jobData.put("type", "EXPORT");
            jobData.put("connectionId", connection.getId());
            jobData.put("organizationId", orgId);
            jobData.put("userId", user.getId());
            jobData.put("fileIds", request.getFileIds());
            jobData.put("targetDriveFolderId", request.getTargetDriveFolderId());
            jobData.put("conflictStrategy", request.getConflictStrategy() != null ? request.getConflictStrategy() : "SKIP");
            jobData.put("accessToken", tokenEncryptor.decrypt(connection.getAccessTokenEncrypted()));
            jobData.put("refreshToken", tokenEncryptor.decrypt(connection.getRefreshTokenEncrypted()));

            String json = objectMapper.writeValueAsString(jobData);
            jobQueueService.push("integration:export", json);

            upsertJobState(jobId, "status", "QUEUED");
            upsertJobState(jobId, "totalItems", String.valueOf(request.getFileIds().size()));
            upsertJobState(jobId, "completedItems", "0");
            upsertJobState(jobId, "failedItems", "0");
            upsertJobState(jobId, "startedAt", Instant.now().toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to queue export job", e);
        }

        return JobResponse.builder()
                .jobId(jobId)
                .status("QUEUED")
                .totalItems(request.getFileIds().size())
                .message("Export job queued")
                .build();
    }

    public Map<String, String> getJobStatus(String jobId) {
        List<Map<String, Object>> rows = pgJdbc.queryForList(
                "SELECT field, value FROM integration_job_state WHERE job_id=?", jobId);
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Job not found");
        }
        Map<String, String> result = new HashMap<>();
        rows.forEach(r -> result.put((String) r.get("field"), (String) r.get("value")));
        result.put("id", jobId);
        return result;
    }

    private void upsertJobState(String jobId, String field, String value) {
        pgJdbc.update("""
                INSERT INTO integration_job_state (job_id, field, value)
                VALUES (?, ?, ?)
                ON CONFLICT (job_id, field) DO UPDATE SET value=EXCLUDED.value, updated_at=NOW()
                """, jobId, field, value);
    }

    private Map<String, String> exchangeCodeForTokens(String code) {
        // In production, this would make an HTTP call to Google's token endpoint
        // For now, return simulated tokens for development
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", "simulated_access_token_" + UUID.randomUUID());
        tokens.put("refresh_token", "simulated_refresh_token_" + UUID.randomUUID());
        tokens.put("email", "user@gmail.com");
        return tokens;
    }

    private DriveBrowseResponse fetchDriveFiles(IntegrationConnection connection, String folderId, String query, String pageToken) {
        // In production, this would call Google Drive API v3
        // For development, return simulated results
        List<DriveItemResponse> items = new ArrayList<>();
        items.add(DriveItemResponse.builder()
                .id("folder_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Documents")
                .mimeType("application/vnd.google-apps.folder")
                .size(0L)
                .modifiedTime(Instant.now().toString())
                .isFolder(true)
                .build());
        items.add(DriveItemResponse.builder()
                .id("file_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Report.pdf")
                .mimeType("application/pdf")
                .size(1048576L)
                .modifiedTime(Instant.now().toString())
                .isFolder(false)
                .build());
        items.add(DriveItemResponse.builder()
                .id("file_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Presentation.pptx")
                .mimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                .size(2097152L)
                .modifiedTime(Instant.now().toString())
                .isFolder(false)
                .build());

        return DriveBrowseResponse.builder()
                .items(items)
                .nextPageToken(null)
                .build();
    }
}
