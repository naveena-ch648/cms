package com.cms.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerCreateRequest {

    @NotBlank(message = "Trigger name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Trigger state is required")
    private String triggerState;

    @NotNull(message = "Trigger type is required")
    private String triggerType;

    private Map<String, Object> config;

    @Builder.Default
    private boolean enabled = true;
}
