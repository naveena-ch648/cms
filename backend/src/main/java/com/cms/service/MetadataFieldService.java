package com.cms.service;

import com.cms.dto.metadata.MetadataFieldRequest;
import com.cms.dto.metadata.MetadataFieldResponse;
import com.cms.entity.MetadataField;
import com.cms.entity.Workspace;
import com.cms.repository.MetadataFieldRepository;
import com.cms.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataFieldService {

    private final MetadataFieldRepository metadataFieldRepository;
    private final WorkspaceRepository workspaceRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "metadata:fields:workspace:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_FIELDS_PER_WORKSPACE = 50;

    @Transactional
    public MetadataFieldResponse createField(Long workspaceId, MetadataFieldRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        long fieldCount = metadataFieldRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        if (fieldCount >= MAX_FIELDS_PER_WORKSPACE) {
            throw new IllegalStateException("Workspace has reached the maximum of " + MAX_FIELDS_PER_WORKSPACE + " metadata fields");
        }

        if (metadataFieldRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(workspaceId, request.getName())) {
            throw new IllegalArgumentException("A field with name '" + request.getName() + "' already exists in this workspace");
        }

        MetadataField.FieldType fieldType = MetadataField.FieldType.valueOf(request.getFieldType().toUpperCase());

        if (fieldType == MetadataField.FieldType.DROPDOWN) {
            if (request.getOptions() == null || request.getOptions().isEmpty()) {
                throw new IllegalArgumentException("Options are required for DROPDOWN field type");
            }
        }

        String optionsJson = null;
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            try {
                optionsJson = objectMapper.writeValueAsString(request.getOptions());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid options format");
            }
        }

        MetadataField field = MetadataField.builder()
                .workspace(workspace)
                .name(request.getName())
                .fieldType(fieldType)
                .description(request.getDescription())
                .options(optionsJson)
                .required(request.getRequired() != null ? request.getRequired() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        field = metadataFieldRepository.save(field);
        invalidateCache(workspaceId);
        log.info("Created metadata field '{}' in workspace {}", field.getName(), workspaceId);
        return MetadataFieldResponse.from(field);
    }

    public List<MetadataFieldResponse> listFields(Long workspaceId, boolean includeDeleted) {
        List<MetadataField> fields;
        if (includeDeleted) {
            fields = metadataFieldRepository.findByWorkspaceIdOrderByDisplayOrder(workspaceId);
        } else {
            fields = metadataFieldRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByDisplayOrder(workspaceId);
        }
        return fields.stream().map(MetadataFieldResponse::from).toList();
    }

    public MetadataField getFieldByUuid(String uuid) {
        return metadataFieldRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + uuid));
    }

    @Transactional
    public MetadataFieldResponse updateField(String fieldUuid, MetadataFieldRequest request) {
        MetadataField field = getFieldByUuid(fieldUuid);

        if (field.isDeleted()) {
            throw new IllegalArgumentException("Cannot update a deleted field");
        }

        if (!field.getName().equalsIgnoreCase(request.getName())) {
            if (metadataFieldRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
                    field.getWorkspace().getId(), request.getName(), field.getId())) {
                throw new IllegalArgumentException("A field with name '" + request.getName() + "' already exists in this workspace");
            }
        }

        field.setName(request.getName());
        field.setDescription(request.getDescription());

        if (request.getOptions() != null) {
            try {
                field.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid options format");
            }
        }

        if (request.getRequired() != null) {
            field.setRequired(request.getRequired());
        }
        if (request.getDisplayOrder() != null) {
            field.setDisplayOrder(request.getDisplayOrder());
        }

        field = metadataFieldRepository.save(field);
        invalidateCache(field.getWorkspace().getId());
        log.info("Updated metadata field '{}'", field.getName());
        return MetadataFieldResponse.from(field);
    }

    @Transactional
    public void deleteField(String fieldUuid) {
        MetadataField field = getFieldByUuid(fieldUuid);
        if (field.isDeleted()) {
            throw new IllegalArgumentException("Field is already deleted");
        }
        field.setDeletedAt(Instant.now());
        metadataFieldRepository.save(field);
        invalidateCache(field.getWorkspace().getId());
        log.info("Soft-deleted metadata field '{}'", field.getName());
    }

    private void invalidateCache(Long workspaceId) {
        String cacheKey = CACHE_KEY_PREFIX + workspaceId;
        redisTemplate.delete(cacheKey);
    }
}
