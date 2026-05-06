package com.cms.dto.organization;

import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationRequest {
    private String name;

    @Email(message = "Invalid email format")
    private String billingContactEmail;
}
