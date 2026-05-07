package com.cms.dto.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataValueRequest {

    @NotEmpty(message = "Values list cannot be empty")
    @Valid
    private List<FieldValue> values;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldValue {
        @NotNull(message = "Field ID is required")
        private String fieldId;

        private Object value;
    }
}
