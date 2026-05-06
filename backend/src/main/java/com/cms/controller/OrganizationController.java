package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.organization.*;
import com.cms.entity.Organization;
import com.cms.service.OrganizationService;
import com.cms.service.PolicyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final PolicyService policyService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody CreateOrganizationRequest request) {
        Organization org = organizationService.create(
                request.getName(), request.getSlug(), request.getBillingContactEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(OrganizationResponse.from(org)));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable String orgId) {
        Organization org = organizationService.getById(orgId);
        return ResponseEntity.ok(ApiResponse.ok(OrganizationResponse.from(org)));
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable String orgId, @Valid @RequestBody UpdateOrganizationRequest request) {
        Organization org = organizationService.update(orgId, request.getName(), request.getBillingContactEmail());
        return ResponseEntity.ok(ApiResponse.ok(OrganizationResponse.from(org)));
    }

    @PutMapping("/{orgId}/policies")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePolicies(
            @PathVariable String orgId, @RequestBody UpdatePoliciesRequest request) throws JsonProcessingException {
        Organization org = organizationService.updatePolicies(orgId, request.getPolicies());
        Map<String, Object> effective = policyService.getEffectivePolicy(org.getPolicies());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("policies", effective)));
    }

    @PutMapping("/{orgId}/deactivate")
    public ResponseEntity<ApiResponse<OrganizationResponse>> deactivate(@PathVariable String orgId) {
        Organization org = organizationService.deactivate(orgId);
        return ResponseEntity.ok(ApiResponse.ok(OrganizationResponse.from(org)));
    }
}
