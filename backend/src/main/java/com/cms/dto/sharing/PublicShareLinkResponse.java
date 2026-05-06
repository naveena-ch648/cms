package com.cms.dto.sharing;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicShareLinkResponse {

    private String resourceType;
    private String resourceName;
    private String mimeType;
    private Long size;
    private boolean allowDownload;
    private boolean watermarkEnabled;
    private String previewUrl;
    private String downloadUrl;
    private boolean requiresPassword;
}
