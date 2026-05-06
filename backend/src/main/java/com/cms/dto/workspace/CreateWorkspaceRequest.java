package com.cms.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 500)
    private String description;
}
