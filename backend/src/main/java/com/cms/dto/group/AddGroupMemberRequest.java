package com.cms.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddGroupMemberRequest {

    @NotBlank(message = "User ID is required")
    private String userId;
}
