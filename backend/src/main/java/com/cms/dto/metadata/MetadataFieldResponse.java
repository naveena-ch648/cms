package com.cms.dto.metadata;

import com.cms.entity.MetadataField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataFieldResponse {

    private String id;
    private String name;
    private String fieldType;
    private String description;
    private List<String> options;
    private Boolean required;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MetadataFieldResponse from(MetadataField field) {
        List<String> opts = null;
        if (field.getOptions() != null) {
            try {
                opts = objectMapper.readValue(field.getOptions(), new TypeReference<>() {});
            } catch (Exception e) {
                opts = List.of();
            }
        }
        return MetadataFieldResponse.builder()
                .id(field.getUuid())
                .name(field.getName())
                .fieldType(field.getFieldType().name())
                .description(field.getDescription())
                .options(opts)
                .required(field.getRequired())
                .displayOrder(field.getDisplayOrder())
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }
}
