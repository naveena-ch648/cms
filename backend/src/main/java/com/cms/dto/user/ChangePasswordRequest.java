package com.cms.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
