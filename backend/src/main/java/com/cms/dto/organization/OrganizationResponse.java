package com.cms.dto.organization;

import com.cms.entity.Organization;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {

    private String id;
    private String name;
    private String slug;
    private String billingContactEmail;
    private String status;
    private Map<String, Object> policies;
    private Instant createdAt;
    private Instant updatedAt;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static OrganizationResponse from(Organization org) {
        Map<String, Object> policiesMap = Map.of();
        if (org.getPolicies() != null && !org.getPolicies().equals("{}")) {
            try {
                policiesMap = mapper.readValue(org.getPolicies(), new TypeReference<>() {});
            } catch (JsonProcessingException ignored) {}
        }

        return OrganizationResponse.builder()
                .id(org.getUuid())
                .name(org.getName())
                .slug(org.getSlug())
                .billingContactEmail(org.getBillingContactEmail())
                .status(org.getStatus().name())
                .policies(policiesMap)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
