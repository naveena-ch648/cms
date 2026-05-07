package com.cms.service;

import com.cms.dto.metadata.MetadataValueRequest;
import com.cms.dto.metadata.MetadataValueResponse;
import com.cms.entity.FileEntity;
import com.cms.entity.MetadataField;
import com.cms.entity.MetadataValue;
import com.cms.repository.FileRepository;
import com.cms.repository.MetadataFieldRepository;
import com.cms.repository.MetadataValueRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataValueService {

    private final MetadataValueRepository metadataValueRepository;
    private final MetadataFieldRepository metadataFieldRepository;
    private final FileRepository fileRepository;
    private final SearchIndexService searchIndexService;
    private final ObjectMapper objectMapper;

    public List<MetadataValueResponse> getFileMetadata(Long fileId) {
        List<MetadataValue> values = metadataValueRepository.findByFileId(fileId);
        return values.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<MetadataValueResponse> updateFileMetadata(Long fileId, MetadataValueRequest request) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        List<MetadataValueResponse> responses = new ArrayList<>();

        for (MetadataValueRequest.FieldValue fieldValue : request.getValues()) {
            MetadataField field = metadataFieldRepository.findByUuid(fieldValue.getFieldId())
                    .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + fieldValue.getFieldId()));

            if (field.isDeleted()) {
                throw new IllegalArgumentException("Cannot assign value to deleted field: " + field.getName());
            }

            if (fieldValue.getValue() == null) {
                if (field.getRequired()) {
                    throw new IllegalArgumentException("Field '" + field.getName() + "' is required and cannot be null");
                }
                metadataValueRepository.findByFileIdAndFieldId(fileId, field.getId())
                        .ifPresent(v -> metadataValueRepository.delete(v));
                continue;
            }

            MetadataValue value = metadataValueRepository.findByFileIdAndFieldId(fileId, field.getId())
                    .orElse(MetadataValue.builder().field(field).file(file).build());

            setTypedValue(value, field, fieldValue.getValue());
            metadataValueRepository.save(value);
            responses.add(toResponse(value));
        }

        updateSearchIndex(file);
        return responses;
    }

    @Transactional
    public void deleteFieldValue(Long fileId, String fieldUuid) {
        MetadataField field = metadataFieldRepository.findByUuid(fieldUuid)
                .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + fieldUuid));

        Optional<MetadataValue> value = metadataValueRepository.findByFileIdAndFieldId(fileId, field.getId());
        if (value.isEmpty()) {
            throw new IllegalArgumentException("No value set for this field on this file");
        }
        metadataValueRepository.delete(value.get());

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        updateSearchIndex(file);
    }

    @Transactional
    public void bulkUpdateMetadata(List<Long> fileIds, List<MetadataValueRequest.FieldValue> values) {
        for (Long fileId : fileIds) {
            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

            for (MetadataValueRequest.FieldValue fieldValue : values) {
                MetadataField field = metadataFieldRepository.findByUuid(fieldValue.getFieldId())
                        .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + fieldValue.getFieldId()));

                if (field.isDeleted()) {
                    throw new IllegalArgumentException("Cannot assign value to deleted field: " + field.getName());
                }

                MetadataValue value = metadataValueRepository.findByFileIdAndFieldId(fileId, field.getId())
                        .orElse(MetadataValue.builder().field(field).file(file).build());

                if (fieldValue.getValue() == null) {
                    if (value.getId() != null) {
                        metadataValueRepository.delete(value);
                    }
                } else {
                    setTypedValue(value, field, fieldValue.getValue());
                    metadataValueRepository.save(value);
                }
            }

            updateSearchIndex(file);
        }
    }

    private void setTypedValue(MetadataValue value, MetadataField field, Object rawValue) {
        value.setTextValue(null);
        value.setNumberValue(null);
        value.setDateValue(null);

        switch (field.getFieldType()) {
            case TEXT -> {
                String textVal = rawValue.toString();
                if (textVal.length() > 1000) {
                    throw new IllegalArgumentException("Text value exceeds maximum length of 1000 characters");
                }
                value.setTextValue(textVal);
            }
            case NUMBER -> {
                try {
                    BigDecimal numVal = new BigDecimal(rawValue.toString());
                    value.setNumberValue(numVal);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number value for field '" + field.getName() + "'");
                }
            }
            case DATE -> {
                try {
                    LocalDate dateVal = LocalDate.parse(rawValue.toString());
                    value.setDateValue(dateVal);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date value for field '" + field.getName() + "'. Expected format: YYYY-MM-DD");
                }
            }
            case DROPDOWN -> {
                String dropVal = rawValue.toString();
                validateDropdownValue(field, dropVal);
                value.setTextValue(dropVal);
            }
        }
    }

    private void validateDropdownValue(MetadataField field, String value) {
        if (field.getOptions() == null) {
            throw new IllegalArgumentException("Dropdown field '" + field.getName() + "' has no options configured");
        }
        try {
            List<String> options = objectMapper.readValue(field.getOptions(), new TypeReference<>() {});
            if (!options.contains(value)) {
                throw new IllegalArgumentException("Value '" + value + "' is not a valid option for field '" + field.getName() + "'");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error validating dropdown options for field '" + field.getName() + "'");
        }
    }

    private void updateSearchIndex(FileEntity file) {
        try {
            List<MetadataValue> values = metadataValueRepository.findByFileId(file.getId());
            searchIndexService.updateFileMetadata(file.getUuid(), values);
        } catch (Exception e) {
            log.warn("Failed to update search index for file {}: {}", file.getUuid(), e.getMessage());
        }
    }

    private MetadataValueResponse toResponse(MetadataValue value) {
        Object responseValue = switch (value.getField().getFieldType()) {
            case TEXT, DROPDOWN -> value.getTextValue();
            case NUMBER -> value.getNumberValue();
            case DATE -> value.getDateValue() != null ? value.getDateValue().toString() : null;
        };

        return MetadataValueResponse.builder()
                .fieldId(value.getField().getUuid())
                .fieldName(value.getField().getName())
                .fieldType(value.getField().getFieldType().name())
                .value(responseValue)
                .updatedAt(value.getUpdatedAt())
                .build();
    }
}
