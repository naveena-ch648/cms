package com.cms.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUserActionRequest {

    @NotEmpty(message = "userIds must not be empty")
    @Size(max = 100, message = "Maximum 100 users per batch")
    private List<String> userIds;

    @NotNull(message = "action is required")
    private BulkAction action;

    private String roleId;

    public enum BulkAction {
        CHANGE_ROLE, ACTIVATE, DEACTIVATE
    }
}
