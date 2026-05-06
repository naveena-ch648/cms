package com.cms.dto.policy;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponse {
    private Map<String, Object> effectivePolicies;
    private Map<String, Object> customOverrides;
    private Map<String, Object> defaults;
}
