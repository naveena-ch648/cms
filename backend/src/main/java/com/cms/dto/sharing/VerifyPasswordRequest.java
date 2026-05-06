package com.cms.dto.sharing;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPasswordRequest {

    @NotBlank(message = "password is required")
    private String password;
}
