package com.cms.dto.workspace;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkspaceRequest {
    private String name;
    private String description;
}
