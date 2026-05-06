package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.organization.UpdatePoliciesRequest;
import com.cms.dto.policy.PolicyResponse;
import com.cms.entity.Organization;
import com.cms.middleware.TenantContext;
import com.cms.service.OrganizationService;
import com.cms.service.PolicyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'view-roles')")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicies() throws JsonProcessingException {
        Long orgId = TenantContext.getCurrentTenant();
        Organization org = organizationService.getByIdInternal(orgId);

        Map<String, Object> effective = policyService.getEffectivePolicy(org.getPolicies());
        Map<String, Object> overrides = Map.of();
        if (org.getPolicies() != null && !org.getPolicies().isBlank() && !org.getPolicies().equals("{}")) {
            overrides = objectMapper.readValue(org.getPolicies(), new TypeReference<>() {});
        }

        PolicyResponse response = PolicyResponse.builder()
                .effectivePolicies(effective)
                .customOverrides(overrides)
                .defaults(policyService.getDefaults())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @PreAuthorize("hasPermission(null, 'manage-roles')")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicies(
            @RequestBody UpdatePoliciesRequest request) throws JsonProcessingException {
        Long orgId = TenantContext.getCurrentTenant();
        Organization org = organizationService.getByIdInternal(orgId);

        String merged = policyService.mergePolicies(org.getPolicies(), request.getPolicies());
        org.setPolicies(merged);
        org = organizationService.update(org.getUuid(), null, null);

        Map<String, Object> effective = policyService.getEffectivePolicy(org.getPolicies());
        Map<String, Object> overrides = objectMapper.readValue(org.getPolicies(), new TypeReference<>() {});

        PolicyResponse response = PolicyResponse.builder()
                .effectivePolicies(effective)
                .customOverrides(overrides)
                .defaults(policyService.getDefaults())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
