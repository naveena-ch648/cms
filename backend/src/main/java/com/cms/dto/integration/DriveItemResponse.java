package com.cms.dto.integration;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveItemResponse {
    private String id;
    private String name;
    private String mimeType;
    private Long size;
    private String modifiedTime;
    private boolean isFolder;
    private String iconLink;
}
