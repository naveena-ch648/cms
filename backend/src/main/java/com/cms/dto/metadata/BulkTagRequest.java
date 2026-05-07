package com.cms.dto.metadata;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkTagRequest {

    @NotEmpty(message = "File IDs list cannot be empty")
    @Size(max = 100, message = "Cannot update more than 100 files at once")
    private List<String> fileIds;

    @NotEmpty(message = "Tags list cannot be empty")
    @Size(max = 20, message = "Cannot add more than 20 tags at once")
    private List<@Size(min = 1, max = 50, message = "Tag must be between 1 and 50 characters") String> tags;
}
