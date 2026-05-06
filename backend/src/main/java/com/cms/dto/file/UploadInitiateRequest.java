package com.cms.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadInitiateRequest {

    @NotBlank
    private String fileName;

    @NotNull
    private Long fileSize;

    @NotBlank
    private String mimeType;

    @NotBlank
    private String folderId;

    private Long chunkSize;

    private String description;

    private String tags;

    private String onDuplicate;
}
