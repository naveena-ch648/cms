package com.cms.service;

import com.cms.dto.metadata.TagResponse;
import com.cms.entity.FileEntity;
import com.cms.entity.FileTag;
import com.cms.entity.User;
import com.cms.entity.Workspace;
import com.cms.repository.FileRepository;
import com.cms.repository.FileTagRepository;
import com.cms.repository.UserRepository;
import com.cms.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private final FileTagRepository fileTagRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SearchIndexService searchIndexService;

    private static final String AUTOCOMPLETE_KEY_PREFIX = "tags:autocomplete:workspace:";
    private static final int MAX_TAGS_PER_FILE = 20;
    private static final int MAX_TAG_LENGTH = 50;

    public List<TagResponse> getFileTags(Long fileId) {
        return fileTagRepository.findByFileId(fileId).stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public List<TagResponse> addTags(Long fileId, Long workspaceId, Long userId, List<String> tagNames) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long currentCount = fileTagRepository.countByFileId(fileId);
        if (currentCount + tagNames.size() > MAX_TAGS_PER_FILE) {
            throw new IllegalStateException("File cannot have more than " + MAX_TAGS_PER_FILE + " tags");
        }

        List<FileTag> addedTags = new ArrayList<>();
        for (String tagName : tagNames) {
            String normalizedName = tagName.trim().toLowerCase();
            if (normalizedName.isEmpty() || normalizedName.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("Tag must be between 1 and " + MAX_TAG_LENGTH + " characters");
            }

            if (fileTagRepository.existsByFileIdAndNameIgnoreCase(fileId, normalizedName)) {
                continue; // idempotent — skip duplicates
            }

            FileTag tag = FileTag.builder()
                    .file(file)
                    .workspace(workspace)
                    .name(normalizedName)
                    .createdBy(user)
                    .build();
            addedTags.add(fileTagRepository.save(tag));
            addToAutocomplete(workspaceId, normalizedName);
        }

        updateSearchIndex(file);
        log.info("Added {} tags to file {}", addedTags.size(), fileId);

        return fileTagRepository.findByFileId(fileId).stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public void removeTag(Long fileId, String tagName) {
        String normalizedName = tagName.trim().toLowerCase();
        FileTag tag = fileTagRepository.findByFileIdAndNameIgnoreCase(fileId, normalizedName)
                .orElseThrow(() -> new IllegalArgumentException("Tag '" + tagName + "' not found on this file"));

        fileTagRepository.delete(tag);

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        updateSearchIndex(file);
        log.info("Removed tag '{}' from file {}", tagName, fileId);
    }

    @Transactional
    public void bulkAddTags(List<Long> fileIds, Long workspaceId, Long userId, List<String> tagNames) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        for (Long fileId : fileIds) {
            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

            long currentCount = fileTagRepository.countByFileId(fileId);

            for (String tagName : tagNames) {
                String normalizedName = tagName.trim().toLowerCase();
                if (normalizedName.isEmpty() || normalizedName.length() > MAX_TAG_LENGTH) {
                    continue;
                }
                if (fileTagRepository.existsByFileIdAndNameIgnoreCase(fileId, normalizedName)) {
                    continue;
                }
                if (currentCount >= MAX_TAGS_PER_FILE) {
                    log.warn("File {} has reached max tag limit, skipping remaining tags", fileId);
                    break;
                }

                FileTag tag = FileTag.builder()
                        .file(file)
                        .workspace(workspace)
                        .name(normalizedName)
                        .createdBy(user)
                        .build();
                fileTagRepository.save(tag);
                addToAutocomplete(workspaceId, normalizedName);
                currentCount++;
            }

            updateSearchIndex(file);
        }
    }

    public List<String> autocomplete(Long workspaceId, String prefix, int limit) {
        String normalizedPrefix = prefix != null ? prefix.trim().toLowerCase() : "";
        List<FileTag> tags = fileTagRepository.findDistinctByWorkspaceIdOrderByName(workspaceId);
        return tags.stream()
                .map(FileTag::getName)
                .filter(name -> normalizedPrefix.isEmpty() || name.startsWith(normalizedPrefix))
                .distinct()
                .limit(Math.min(limit, 50))
                .collect(Collectors.toList());
    }

    private void addToAutocomplete(Long workspaceId, String tagName) {
        // No-op: autocomplete now done via DB query
    }

    private void updateSearchIndex(FileEntity file) {
        try {
            List<FileTag> tags = fileTagRepository.findByFileId(file.getId());
            List<String> tagNames = tags.stream().map(FileTag::getName).toList();
            searchIndexService.updateFileTags(file.getUuid(), tagNames);
        } catch (Exception e) {
            log.warn("Failed to update search index tags for file {}: {}", file.getUuid(), e.getMessage());
        }
    }
}
