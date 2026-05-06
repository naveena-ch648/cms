package com.cms.dto.group;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGroupRequest {
    private String name;
    private String description;
}
