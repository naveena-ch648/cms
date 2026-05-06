package com.cms.dto.folder;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFolderRequest {

    @Size(min = 1, max = 255, message = "Folder name must be between 1 and 255 characters")
    private String name;

    private Integer sortOrder;
}
