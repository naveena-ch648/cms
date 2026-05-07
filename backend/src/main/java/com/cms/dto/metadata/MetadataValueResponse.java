package com.cms.dto.metadata;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataValueResponse {

    private String fieldId;
    private String fieldName;
    private String fieldType;
    private Object value;
    private Instant updatedAt;
}
