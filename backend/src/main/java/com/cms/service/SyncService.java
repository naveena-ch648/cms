package com.cms.service;

import com.cms.dto.integration.*;
import com.cms.entity.*;
import com.cms.middleware.TenantContext;
import com.cms.repository.*;
import com.cms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final SyncLinkRepository syncLinkRepository;
    private final SyncJobRepository syncJobRepository;
    private final IntegrationConnectionRepository connectionRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public SyncLinkResponse createSyncLink(SyncLinkRequest request, UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();

        IntegrationConnection connection = connectionRepository.findByUuid(request.getConnectionId())
                .orElseThrow(() -> new NoSuchElementException("Connection not found"));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to use this connection");
        }

        Folder folder = folderRepository.findByUuid(request.getFolderId())
                .orElseThrow(() -> new NoSuchElementException("Folder not found"));

        // Check if sync link already exists for this folder
        Optional<SyncLink> existing = syncLinkRepository.findByFolderId(folder.getId());
        if (existing.isPresent()) {
            throw new IllegalStateException("A sync link already exists for this folder");
        }

        Organization org = new Organization();
        org.setId(orgId);

        SyncLink.Direction direction = request.getDirection() != null
                ? SyncLink.Direction.valueOf(request.getDirection())
                : SyncLink.Direction.BIDIRECTIONAL;

        int syncInterval = request.getSyncIntervalMinutes() != null
                ? request.getSyncIntervalMinutes()
                : 15;

        SyncLink syncLink = SyncLink.builder()
                .uuid(UUID.randomUUID().toString())
                .organization(org)
                .connection(connection)
                .folder(folder)
                .externalFolderId(request.getExternalFolderId())
                .externalFolderName(request.getExternalFolderName())
                .direction(direction)
                .syncIntervalMinutes(syncInterval)
                .status(SyncLink.Status.ACTIVE)
                .nextSyncAt(Instant.now().plusSeconds(syncInterval * 60L))
                .build();

        syncLink = syncLinkRepository.save(syncLink);
        return toResponse(syncLink);
    }

    public List<SyncLinkResponse> listSyncLinks(UserPrincipal user) {
        Long orgId = TenantContext.getCurrentTenant();
        List<SyncLink> links = syncLinkRepository.findByOrganizationId(orgId);
        return links.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SyncLinkResponse updateSyncLink(String syncLinkId, SyncLinkUpdateRequest request, UserPrincipal user) {
        SyncLink syncLink = syncLinkRepository.findByUuid(syncLinkId)
                .orElseThrow(() -> new NoSuchElementException("Sync link not found"));

        if (!syncLink.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        if (request.getDirection() != null) {
            syncLink.setDirection(SyncLink.Direction.valueOf(request.getDirection()));
        }
        if (request.getSyncIntervalMinutes() != null) {
            syncLink.setSyncIntervalMinutes(request.getSyncIntervalMinutes());
            syncLink.setNextSyncAt(Instant.now().plusSeconds(request.getSyncIntervalMinutes() * 60L));
        }
        if (request.getStatus() != null) {
            syncLink.setStatus(SyncLink.Status.valueOf(request.getStatus()));
        }

        syncLink = syncLinkRepository.save(syncLink);
        return toResponse(syncLink);
    }

    @Transactional
    public void deleteSyncLink(String syncLinkId, UserPrincipal user) {
        SyncLink syncLink = syncLinkRepository.findByUuid(syncLinkId)
                .orElseThrow(() -> new NoSuchElementException("Sync link not found"));

        if (!syncLink.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        syncLinkRepository.delete(syncLink);
    }

    public Page<SyncJobResponse> getSyncJobs(String syncLinkId, Pageable pageable) {
        SyncLink syncLink = syncLinkRepository.findByUuid(syncLinkId)
                .orElseThrow(() -> new NoSuchElementException("Sync link not found"));

        if (!syncLink.getOrganization().getId().equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Not authorized");
        }

        return syncJobRepository.findBySyncLinkIdOrderByStartedAtDesc(syncLink.getId(), pageable)
                .map(this::toJobResponse);
    }

    private SyncLinkResponse toResponse(SyncLink link) {
        return SyncLinkResponse.builder()
                .id(link.getUuid())
                .folderId(link.getFolder().getUuid())
                .folderName(link.getFolder().getName())
                .externalFolderName(link.getExternalFolderName())
                .provider(link.getConnection().getProvider())
                .direction(link.getDirection().name())
                .syncIntervalMinutes(link.getSyncIntervalMinutes())
                .status(link.getStatus().name())
                .lastSyncAt(link.getLastSyncAt())
                .nextSyncAt(link.getNextSyncAt())
                .build();
    }

    private SyncJobResponse toJobResponse(SyncJob job) {
        return SyncJobResponse.builder()
                .id(job.getUuid())
                .status(job.getStatus().name())
                .direction(job.getDirection())
                .itemsSynced(job.getItemsSynced())
                .itemsFailed(job.getItemsFailed())
                .itemsConflicted(job.getItemsConflicted())
                .bytesTransferred(job.getBytesTransferred())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
