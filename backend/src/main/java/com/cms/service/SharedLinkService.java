package com.cms.service;

import com.cms.dto.sharing.*;
import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedLinkService {

    private final SharedLinkRepository sharedLinkRepository;
    private final SharedLinkAccessRepository sharedLinkAccessRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PermissionFilterService permissionFilterService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SHARE_CACHE_PREFIX = "share_link:";
    private static final long SHARE_CACHE_TTL_MINUTES = 2;
    private static final int TOKEN_BYTES = 32;
    private static final Duration DOWNLOAD_URL_EXPIRY = Duration.ofMinutes(15);

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public SharedLink createLink(CreateShareLinkRequest request, Long userId, String workspaceUuid) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        SharedLink.ResourceType resourceType;
        try {
            resourceType = SharedLink.ResourceType.valueOf(request.getResourceType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_SHARE", "Invalid resource type: " + request.getResourceType());
        }

        FileEntity file = null;
        Folder folder = null;

        if (resourceType == SharedLink.ResourceType.FILE) {
            if (request.getFileUuid() == null) {
                throw new BusinessRuleException("INVALID_SHARE", "fileUuid is required for FILE resource type");
            }
            file = fileRepository.findByUuid(request.getFileUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("File not found"));
            if (!permissionFilterService.hasEditorAccess(userId, file.getFolder())) {
                throw new BusinessRuleException("INSUFFICIENT_PERMISSION", "Editor access required to create share links");
            }
        } else {
            if (request.getFolderUuid() == null) {
                throw new BusinessRuleException("INVALID_SHARE", "folderUuid is required for FOLDER resource type");
            }
            folder = folderRepository.findByUuid(request.getFolderUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
            if (!permissionFilterService.hasEditorAccess(userId, folder)) {
                throw new BusinessRuleException("INSUFFICIENT_PERMISSION", "Editor access required to create share links");
            }
        }

        // Generate cryptographic token
        String token = generateToken();

        // Hash password if provided
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        SharedLink link = SharedLink.builder()
                .uuid(UUID.randomUUID().toString())
                .token(token)
                .resourceType(resourceType)
                .file(file)
                .folder(folder)
                .createdBy(creator)
                .workspace(workspace)
                .passwordHash(passwordHash)
                .expiresAt(request.getExpiresAt())
                .allowDownload(request.isAllowDownload())
                .watermarkEnabled(request.isWatermarkEnabled())
                .build();

        link = sharedLinkRepository.save(link);

        auditService.log(workspace.getOrganization(), creator, "SHARE_LINK_CREATED",
                "SharedLink", link.getId(),
                "Created share link for " + request.getResourceType(), null);

        return link;
    }

    public SharedLink accessLink(String token) {
        // Check Redis cache first
        String cacheKey = SHARE_CACHE_PREFIX + token;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                // Cache hit — still validate status
                SharedLink link = sharedLinkRepository.findByToken(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
                validateLinkAccess(link);
                return link;
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
                throw e;
            }
        }

        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        validateLinkAccess(link);

        // Cache the token validation
        try {
            redisTemplate.opsForValue().set(cacheKey, "valid", SHARE_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }

        return link;
    }

    @Transactional
    public SharedLink recordAccess(String token, String ipAddress, String userAgent) {
        SharedLink link = accessLink(token);

        // Increment view count
        link.setViewCount(link.getViewCount() + 1);
        link.setLastAccessedAt(Instant.now());
        sharedLinkRepository.save(link);

        // Record access log
        SharedLinkAccess access = SharedLinkAccess.builder()
                .sharedLink(link)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        sharedLinkAccessRepository.save(access);

        return link;
    }

    public boolean verifyPassword(String token, String password) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        if (link.getPasswordHash() == null) {
            return true; // No password required
        }
        return passwordEncoder.matches(password, link.getPasswordHash());
    }

    @Transactional
    public void revokeLink(String uuid, Long userId) {
        SharedLink link = sharedLinkRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        // Only creator or workspace admin can revoke
        if (!link.getCreatedBy().getId().equals(userId)) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION", "Only the creator can revoke this link");
        }

        link.setStatus(SharedLink.LinkStatus.REVOKED);
        sharedLinkRepository.save(link);

        // Invalidate cache
        redisTemplate.delete(SHARE_CACHE_PREFIX + link.getToken());

        auditService.log(link.getWorkspace().getOrganization(), link.getCreatedBy(),
                "SHARE_LINK_REVOKED", "SharedLink", link.getId(),
                "Revoked share link", null);
    }

    public String generateDownloadUrl(String token) {
        SharedLink link = accessLink(token);

        if (!link.isAllowDownload()) {
            throw new BusinessRuleException("DOWNLOAD_DISABLED", "Download is not allowed for this share link");
        }

        if (link.getResourceType() == SharedLink.ResourceType.FILE && link.getFile() != null) {
            FileEntity file = link.getFile();
            return storageService.presignGetUrl(file.getStorageBucket(), file.getStorageKey(), DOWNLOAD_URL_EXPIRY);
        }

        throw new BusinessRuleException("DOWNLOAD_UNAVAILABLE", "Download not available for this resource");
    }

    public Page<SharedLink> listLinks(Long userId, String workspaceUuid, SharedLink.LinkStatus status, Pageable pageable) {
        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (status != null) {
            return sharedLinkRepository.findByCreatedByIdAndWorkspaceIdAndStatus(userId, workspace.getId(), status, pageable);
        }
        return sharedLinkRepository.findByCreatedByIdAndWorkspaceId(userId, workspace.getId(), pageable);
    }

    @Transactional
    public SharedLink updateLink(String uuid, Long userId, UpdateShareLinkRequest request) {
        SharedLink link = sharedLinkRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!link.getCreatedBy().getId().equals(userId)) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION", "Only the creator can update this link");
        }

        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                link.setPasswordHash(null); // Remove password
            } else {
                link.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }
        }
        if (request.getExpiresAt() != null) {
            link.setExpiresAt(request.getExpiresAt());
            // Reactivate if expired but new expiry is in the future
            if (link.getStatus() == SharedLink.LinkStatus.EXPIRED && request.getExpiresAt().isAfter(Instant.now())) {
                link.setStatus(SharedLink.LinkStatus.ACTIVE);
            }
        }
        if (request.getAllowDownload() != null) {
            link.setAllowDownload(request.getAllowDownload());
        }
        if (request.getWatermarkEnabled() != null) {
            link.setWatermarkEnabled(request.getWatermarkEnabled());
        }

        link = sharedLinkRepository.save(link);

        // Invalidate cache
        redisTemplate.delete(SHARE_CACHE_PREFIX + link.getToken());

        return link;
    }

    public Page<SharedLinkAccess> getAccessLog(String uuid, Long userId, Pageable pageable) {
        SharedLink link = sharedLinkRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!link.getCreatedBy().getId().equals(userId)) {
            throw new BusinessRuleException("INSUFFICIENT_PERMISSION", "Only the creator can view access logs");
        }

        return sharedLinkAccessRepository.findBySharedLinkId(link.getId(), pageable);
    }

    private void validateLinkAccess(SharedLink link) {
        if (link.getStatus() == SharedLink.LinkStatus.REVOKED) {
            throw new BusinessRuleException("LINK_REVOKED", "This share link has been revoked");
        }
        if (link.isExpired()) {
            throw new BusinessRuleException("LINK_EXPIRED", "This share link has expired");
        }
        if (link.getMaxViews() != null && link.getViewCount() >= link.getMaxViews()) {
            throw new BusinessRuleException("MAX_VIEWS_REACHED", "This share link has reached its maximum view count");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(TOKEN_BYTES * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
