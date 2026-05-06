package com.cms.dto.folder;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveFolderRequest {

    private String targetParentId;

    @Builder.Default
    private Integer sortOrder = 0;
}
