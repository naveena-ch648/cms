package com.cms.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptClassificationRequest {
    @NotBlank
    private String category;
}
