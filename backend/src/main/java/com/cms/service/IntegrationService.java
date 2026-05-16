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

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;

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
    public ConnectionResponse handleOAuthCallback(String code, String state) {
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

        String[] parts = stateValue.split(":");
        Long stateUserId = Long.parseLong(parts[0]);
        Long stateOrgId = 1L; // Fallback default
        if (parts.length > 1 && !"null".equals(parts[1])) {
            try {
                stateOrgId = Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        // Exchange code for tokens (simulated — in production would call Google Token endpoint)
        Map<String, String> tokens = exchangeCodeForTokens(code);
        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.get("refresh_token");
        String email = tokens.getOrDefault("email", "connected@google.com");

        // Check for existing connection
        Optional<IntegrationConnection> existing = connectionRepository
                .findByOrganizationIdAndUserIdAndProvider(stateOrgId, stateUserId, "GOOGLE_DRIVE");
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
            org.setId(stateOrgId);
            User userEntity = new User();
            userEntity.setId(stateUserId);

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
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", googleRedirectUri);
        body.add("grant_type", "authorization_code");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL, new HttpEntity<>(body, headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            
            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", json.path("access_token").asText());
            if (json.has("refresh_token")) {
                tokens.put("refresh_token", json.path("refresh_token").asText());
            } else {
                tokens.put("refresh_token", "");
            }
            
            // Fetch email from Google UserInfo
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(tokens.get("access_token"));
            ResponseEntity<String> userInfo = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo", 
                    HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
            JsonNode infoJson = objectMapper.readTree(userInfo.getBody());
            tokens.put("email", infoJson.path("email").asText("connected@google.com"));
            
            return tokens;
        } catch (Exception e) {
            log.error("Failed to exchange code for tokens", e);
            throw new RuntimeException("Failed to exchange OAuth code for tokens", e);
        }
    }

    private DriveBrowseResponse fetchDriveFiles(IntegrationConnection connection, String folderId, String query, String pageToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        try {
            String accessToken = tokenEncryptor.decrypt(connection.getAccessTokenEncrypted());
            headers.setBearerAuth(accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt access token", e);
        }

        String q = "trashed = false";
        if (folderId != null && !folderId.trim().isEmpty() && !folderId.equals("root")) {
            q += " and '" + folderId + "' in parents";
        } else {
            q += " and 'root' in parents";
        }
        if (query != null && !query.trim().isEmpty()) {
            q += " and name contains '" + query.replace("'", "\\'") + "'";
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/drive/v3/files")
                .queryParam("q", q)
                .queryParam("fields", "nextPageToken, files(id, name, mimeType, size, modifiedTime, parents)")
                .queryParam("pageSize", "50")
                .queryParam("orderBy", "folder, name");

        if (pageToken != null && !pageToken.trim().isEmpty()) {
            builder.queryParam("pageToken", pageToken);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            
            List<DriveItemResponse> items = new ArrayList<>();
            if (json.has("files")) {
                for (JsonNode file : json.get("files")) {
                    String mimeType = file.path("mimeType").asText("");
                    boolean isFolder = "application/vnd.google-apps.folder".equals(mimeType);
                    
                    items.add(DriveItemResponse.builder()
                            .id(file.path("id").asText())
                            .name(file.path("name").asText())
                            .mimeType(mimeType)
                            .size(file.path("size").asLong(0L))
                            .modifiedTime(file.path("modifiedTime").asText(Instant.now().toString()))
                            .isFolder(isFolder)
                            .build());
                }
            }
            
            return DriveBrowseResponse.builder()
                    .items(items)
                    .nextPageToken(json.path("nextPageToken").asText(null))
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch Google Drive files", e);
            throw new RuntimeException("Failed to fetch Google Drive files", e);
        }
    }
}
