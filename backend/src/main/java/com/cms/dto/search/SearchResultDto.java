package com.cms.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

    private String fileUuid;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String ownerUuid;
    private String ownerName;
    private String folderPath;
    private String folderUuid;
    private String createdAt;
    private String updatedAt;
    private List<String> highlights;
    private Double score;
}
