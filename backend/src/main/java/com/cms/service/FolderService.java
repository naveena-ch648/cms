package com.cms.service;

import com.cms.dto.folder.FolderResponse;
import com.cms.dto.folder.FolderTreeResponse;
import com.cms.entity.Folder;
import com.cms.entity.FolderFavorite;
import com.cms.entity.FolderRecent;
import com.cms.entity.User;
import com.cms.entity.Workspace;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FolderFavoriteRepository;
import com.cms.repository.FolderRecentRepository;
import com.cms.repository.FolderRepository;
import com.cms.repository.UserRepository;
import com.cms.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderFavoriteRepository folderFavoriteRepository;
    private final FolderRecentRepository folderRecentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String FOLDER_TREE_CACHE_PREFIX = "folder_tree:";
    private static final long CACHE_TTL_MINUTES = 10;

    @Transactional
    public Folder create(String workspaceUuid, String name, String parentUuid, Integer sortOrder, Long creatorUserId) {
        validateFolderName(name);

        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        Folder parent = null;
        if (parentUuid != null) {
            parent = folderRepository.findByUuid(parentUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
            if (!parent.getWorkspace().getId().equals(workspace.getId())) {
                throw new BusinessRuleException("INVALID_PARENT", "Parent folder belongs to a different workspace");
            }
        }

        checkDuplicateName(workspace.getId(), parent != null ? parent.getId() : null, name);

        User creator = creatorUserId != null
                ? userRepository.findById(creatorUserId).orElse(null)
                : null;

        Folder folder = Folder.builder()
                .workspace(workspace)
                .parent(parent)
                .name(name)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .createdBy(creator)
                .build();

        folder = folderRepository.save(folder);
        invalidateTreeCache(workspace.getId());
        auditService.log(workspace.getOrganization(), creator, "FOLDER_CREATED",
                "Folder", folder.getId(), "Created folder: " + name, null);
        return folder;
    }

    public Folder getByUuid(String uuid) {
        return folderRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
    }

    public List<FolderTreeResponse> listByWorkspace(String workspaceUuid, boolean lazy) {
        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Check cache first
        String cacheKey = FOLDER_TREE_CACHE_PREFIX + workspace.getId();
        if (!lazy) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, new TypeReference<List<FolderTreeResponse>>() {});
                } catch (JsonProcessingException e) {
                    // Cache corrupted, proceed to DB query
                }
            }
        }

        List<Folder> folders;
        if (lazy) {
            folders = folderRepository.findByWorkspaceIdAndParentIsNullAndStatusOrderBySortOrder(
                    workspace.getId(), Folder.FolderStatus.ACTIVE);
        } else {
            folders = folderRepository.findByWorkspaceIdAndStatusOrderBySortOrder(
                    workspace.getId(), Folder.FolderStatus.ACTIVE);
        }

        List<FolderTreeResponse> responses = folders.stream()
                .map(f -> FolderTreeResponse.from(f,
                        folderRepository.countByParentIdAndStatus(f.getId(), Folder.FolderStatus.ACTIVE)))
                .toList();

        // Cache full tree (not lazy)
        if (!lazy) {
            try {
                String json = objectMapper.writeValueAsString(responses);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (JsonProcessingException e) {
                // Log but don't fail on cache write error
            }
        }

        return responses;
    }

    public List<FolderTreeResponse> getChildren(String folderUuid) {
        Folder folder = getByUuid(folderUuid);
        List<Folder> children = folderRepository.findByParentIdAndStatusOrderBySortOrder(
                folder.getId(), Folder.FolderStatus.ACTIVE);
        return children.stream()
                .map(f -> FolderTreeResponse.from(f,
                        folderRepository.countByParentIdAndStatus(f.getId(), Folder.FolderStatus.ACTIVE)))
                .toList();
    }

    public FolderResponse getByUuidWithBreadcrumbs(String uuid) {
        Folder folder = getByUuid(uuid);
        List<FolderResponse.BreadcrumbItem> breadcrumbs = buildBreadcrumbs(folder);
        return FolderResponse.from(folder, breadcrumbs);
    }

    @Transactional
    public Folder update(String uuid, String name, Integer sortOrder) {
        Folder folder = getByUuid(uuid);

        if (name != null) {
            validateFolderName(name);
            if (!name.equalsIgnoreCase(folder.getName())) {
                Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
                checkDuplicateName(folder.getWorkspace().getId(), parentId, name);
            }
            folder.setName(name);
        }

        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }

        folder = folderRepository.save(folder);
        invalidateTreeCache(folder.getWorkspace().getId());
        auditService.log(folder.getWorkspace().getOrganization(), null, "FOLDER_UPDATED",
                "Folder", folder.getId(), "Updated folder: " + folder.getName(), null);
        return folder;
    }

    @Transactional
    public void delete(String uuid) {
        Folder folder = getByUuid(uuid);
        folder.setStatus(Folder.FolderStatus.DELETED);
        folderRepository.save(folder);
        folderRepository.softDeleteDescendants(folder.getId());
        invalidateTreeCache(folder.getWorkspace().getId());
        auditService.log(folder.getWorkspace().getOrganization(), null, "FOLDER_DELETED",
                "Folder", folder.getId(), "Deleted folder: " + folder.getName(), null);
    }

    @Transactional
    public Folder move(String folderUuid, String targetParentUuid, Integer sortOrder) {
        Folder folder = getByUuid(folderUuid);

        Folder targetParent = null;
        if (targetParentUuid != null) {
            targetParent = folderRepository.findByUuid(targetParentUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Target parent folder not found"));
            if (!targetParent.getWorkspace().getId().equals(folder.getWorkspace().getId())) {
                throw new BusinessRuleException("INVALID_TARGET", "Target parent belongs to a different workspace");
            }
            // Circular move check: target cannot be a descendant of the folder being moved
            List<Long> match = folderRepository.findAncestorMatch(targetParent.getId(), folder.getId());
            if (!match.isEmpty()) {
                throw new BusinessRuleException("CIRCULAR_MOVE",
                        "Cannot move folder into one of its own descendants");
            }
            // Also cannot move to itself
            if (folder.getId().equals(targetParent.getId())) {
                throw new BusinessRuleException("CIRCULAR_MOVE", "Cannot move folder into itself");
            }
        }

        // Check name conflict at target
        Long targetParentId = targetParent != null ? targetParent.getId() : null;
        Long currentParentId = folder.getParent() != null ? folder.getParent().getId() : null;
        boolean parentChanged = !java.util.Objects.equals(targetParentId, currentParentId);

        if (parentChanged) {
            checkDuplicateName(folder.getWorkspace().getId(), targetParentId, folder.getName());
        }

        folder.setParent(targetParent);
        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }

        folder = folderRepository.save(folder);
        invalidateTreeCache(folder.getWorkspace().getId());
        auditService.log(folder.getWorkspace().getOrganization(), null, "FOLDER_MOVED",
                "Folder", folder.getId(),
                "Moved folder: " + folder.getName() + " to " + (targetParentUuid != null ? targetParentUuid : "root"),
                null);
        return folder;
    }

    public List<FolderResponse.BreadcrumbItem> getAncestorPath(String folderUuid) {
        Folder folder = getByUuid(folderUuid);
        return buildBreadcrumbs(folder);
    }

    private List<FolderResponse.BreadcrumbItem> buildBreadcrumbs(Folder folder) {
        List<FolderResponse.BreadcrumbItem> breadcrumbs = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            breadcrumbs.add(FolderResponse.BreadcrumbItem.builder()
                    .id(current.getUuid())
                    .name(current.getName())
                    .build());
            current = current.getParent();
        }
        Collections.reverse(breadcrumbs);
        return breadcrumbs;
    }

    private void validateFolderName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("INVALID_NAME", "Folder name is required");
        }
        if (name.length() > 255) {
            throw new BusinessRuleException("INVALID_NAME", "Folder name must not exceed 255 characters");
        }
        if (name.contains("/") || name.contains("\\") || name.contains("\0")) {
            throw new BusinessRuleException("INVALID_NAME", "Folder name must not contain path separators");
        }
    }

    // --- Favorites ---

    @Transactional
    public void addFavorite(String folderUuid, Long userId) {
        Folder folder = getByUuid(folderUuid);
        if (folderFavoriteRepository.existsByUserIdAndFolderId(userId, folder.getId())) {
            return; // Already favorited
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        FolderFavorite fav = FolderFavorite.builder()
                .user(user)
                .folder(folder)
                .build();
        folderFavoriteRepository.save(fav);
    }

    @Transactional
    public void removeFavorite(String folderUuid, Long userId) {
        Folder folder = getByUuid(folderUuid);
        folderFavoriteRepository.findByUserIdAndFolderId(userId, folder.getId())
                .ifPresent(folderFavoriteRepository::delete);
    }

    public List<FolderFavorite> listFavorites(Long userId, String workspaceUuid) {
        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return folderFavoriteRepository.findByUserIdAndFolder_WorkspaceIdOrderByCreatedAtDesc(
                userId, workspace.getId());
    }

    // --- Recents ---

    @Transactional
    public void recordVisit(String folderUuid, Long userId) {
        Folder folder = getByUuid(folderUuid);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<FolderRecent> existing = folderRecentRepository.findByUserIdAndFolderId(userId, folder.getId());
        if (existing.isPresent()) {
            FolderRecent recent = existing.get();
            recent.setAccessedAt(java.time.Instant.now());
            folderRecentRepository.save(recent);
            return;
        }

        // Cap at 10 per workspace
        long count = folderRecentRepository.countByUserIdAndFolder_WorkspaceId(userId, folder.getWorkspace().getId());
        if (count >= 10) {
            FolderRecent oldest = folderRecentRepository.findFirstByUserIdAndFolder_WorkspaceIdOrderByAccessedAtAsc(
                    userId, folder.getWorkspace().getId());
            if (oldest != null) {
                folderRecentRepository.delete(oldest);
            }
        }

        FolderRecent recent = FolderRecent.builder()
                .user(user)
                .folder(folder)
                .accessedAt(java.time.Instant.now())
                .build();
        folderRecentRepository.save(recent);
    }

    public List<FolderRecent> listRecents(Long userId, String workspaceUuid) {
        Workspace workspace = workspaceRepository.findByUuid(workspaceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return folderRecentRepository.findTop10ByUserIdAndFolder_WorkspaceIdOrderByAccessedAtDesc(
                userId, workspace.getId());
    }

    private void checkDuplicateName(Long workspaceId, Long parentId, String name) {
        boolean exists;
        if (parentId == null) {
            exists = folderRepository.existsByWorkspaceIdAndParentIsNullAndNameIgnoreCaseAndStatus(
                    workspaceId, name, Folder.FolderStatus.ACTIVE);
        } else {
            exists = folderRepository.existsByWorkspaceIdAndParentIdAndNameIgnoreCaseAndStatus(
                    workspaceId, parentId, name, Folder.FolderStatus.ACTIVE);
        }
        if (exists) {
            throw new DuplicateResourceException("DUPLICATE_NAME",
                    "A folder with the name '" + name + "' already exists in this location");
        }
    }

    private void invalidateTreeCache(Long workspaceId) {
        redisTemplate.delete(FOLDER_TREE_CACHE_PREFIX + workspaceId);
    }
}
