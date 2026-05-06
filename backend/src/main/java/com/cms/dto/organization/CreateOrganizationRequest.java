package com.cms.dto.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrganizationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    @NotBlank(message = "Billing contact email is required")
    @Email(message = "Invalid email format")
    private String billingContactEmail;
}
