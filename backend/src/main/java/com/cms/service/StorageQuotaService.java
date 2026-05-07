package com.cms.service;

import com.cms.dto.admin.StorageQuotaDetailResponse;
import com.cms.dto.admin.StorageQuotaUpdateRequest;
import com.cms.entity.StorageQuota;
import com.cms.exception.BusinessRuleException;
import com.cms.repository.StorageQuotaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageQuotaService {

    private final StorageQuotaRepository storageQuotaRepository;
    private final ObjectMapper objectMapper;

    public StorageQuota getQuotaForOrg(Long organizationId) {
        return storageQuotaRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new IllegalStateException("No storage quota found for organization: " + organizationId));
    }

    public boolean checkQuotaAvailable(Long organizationId, long additionalBytes) {
        StorageQuota quota = getQuotaForOrg(organizationId);
        return (quota.getUsedStorageBytes() + additionalBytes) <= quota.getMaxStorageBytes();
    }

    public void validateFileSize(Long organizationId, long fileSize) {
        StorageQuota quota = getQuotaForOrg(organizationId);
        if (fileSize > quota.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "File size " + fileSize + " exceeds maximum allowed size of " + quota.getMaxFileSizeBytes());
        }
    }

    public void validateFileExtension(Long organizationId, String fileName) {
        StorageQuota quota = getQuotaForOrg(organizationId);
        String extension = getFileExtension(fileName).toLowerCase();

        List<String> blocked = parseJsonList(quota.getBlockedExtensions());
        if (blocked.contains(extension)) {
            throw new IllegalArgumentException("File extension '" + extension + "' is blocked");
        }

        List<String> allowed = parseJsonList(quota.getAllowedExtensions());
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(extension)) {
            throw new IllegalArgumentException("File extension '" + extension + "' is not in the allowed list");
        }
    }

    @Transactional
    public void updateUsedStorage(Long organizationId, long deltaBytes) {
        StorageQuota quota = getQuotaForOrg(organizationId);
        quota.setUsedStorageBytes(Math.max(0, quota.getUsedStorageBytes() + deltaBytes));
        storageQuotaRepository.save(quota);
    }

    @Transactional
    public StorageQuotaDetailResponse updateQuota(Long organizationId, StorageQuotaUpdateRequest request) {
        StorageQuota quota = getQuotaForOrg(organizationId);

        // Validate
        if (request.getMaxStorageBytes() != null && request.getMaxStorageBytes() <= 0) {
            throw new BusinessRuleException("VALIDATION_ERROR", "maxStorageBytes must be positive");
        }
        if (request.getMaxFileSizeBytes() != null && request.getMaxFileSizeBytes() <= 0) {
            throw new BusinessRuleException("VALIDATION_ERROR", "maxFileSizeBytes must be positive");
        }
        if (request.getTrashRetentionDays() != null &&
                (request.getTrashRetentionDays() < 1 || request.getTrashRetentionDays() > 365)) {
            throw new BusinessRuleException("VALIDATION_ERROR", "trashRetentionDays must be between 1 and 365");
        }
        if (request.getAllowedExtensions() != null && request.getBlockedExtensions() != null) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "allowedExtensions and blockedExtensions cannot both be set");
        }

        // Update only provided fields
        if (request.getMaxStorageBytes() != null) {
            quota.setMaxStorageBytes(request.getMaxStorageBytes());
        }
        if (request.getMaxFileSizeBytes() != null) {
            quota.setMaxFileSizeBytes(request.getMaxFileSizeBytes());
        }
        if (request.getTrashRetentionDays() != null) {
            quota.setTrashRetentionDays(request.getTrashRetentionDays());
        }
        if (request.getAllowedExtensions() != null) {
            quota.setAllowedExtensions(toJson(request.getAllowedExtensions()));
        }
        if (request.getBlockedExtensions() != null) {
            quota.setBlockedExtensions(toJson(request.getBlockedExtensions()));
        }

        storageQuotaRepository.save(quota);
        return StorageQuotaDetailResponse.from(quota, objectMapper);
    }

    public StorageQuotaDetailResponse getQuotaDetail(Long organizationId) {
        StorageQuota quota = getQuotaForOrg(organizationId);
        return StorageQuotaDetailResponse.from(quota, objectMapper);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize extensions list: {}", e.getMessage());
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse extensions JSON: {}", json, e);
            return Collections.emptyList();
        }
    }
}
