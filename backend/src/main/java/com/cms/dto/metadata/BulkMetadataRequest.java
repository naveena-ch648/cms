package com.cms.dto.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkMetadataRequest {

    @NotEmpty(message = "File IDs list cannot be empty")
    @Size(max = 100, message = "Cannot update more than 100 files at once")
    private List<String> fileIds;

    @NotEmpty(message = "Values list cannot be empty")
    @Valid
    private List<MetadataValueRequest.FieldValue> values;
}
