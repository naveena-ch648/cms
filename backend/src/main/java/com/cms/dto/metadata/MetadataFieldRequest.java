package com.cms.dto.metadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataFieldRequest {

    @NotBlank(message = "Field name is required")
    @Size(min = 1, max = 100, message = "Field name must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "Field type is required")
    private String fieldType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private List<String> options;

    private Boolean required;

    private Integer displayOrder;
}
