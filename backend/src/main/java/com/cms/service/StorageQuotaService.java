package com.cms.service;

import com.cms.entity.StorageQuota;
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
