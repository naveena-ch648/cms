package com.cms.dto.organization;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePoliciesRequest {
    private Map<String, Object> policies;
}
