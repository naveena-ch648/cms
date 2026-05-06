package com.cms.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddGroupMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
