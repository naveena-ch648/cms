package com.cms.service;

import com.cms.entity.Organization;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.OrganizationRepository;
import com.cms.repository.PermissionRepository;
import com.cms.repository.RoleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PolicyService policyService;

    @Transactional
    public Organization create(String name, String slug, String billingContactEmail) {
        if (organizationRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("ORG_SLUG_EXISTS", "Organization slug already exists");
        }

        Organization org = Organization.builder()
                .name(name)
                .slug(slug)
                .billingContactEmail(billingContactEmail)
                .build();
        org = organizationRepository.save(org);

        seedDefaultRoles(org);
        return org;
    }

    public Organization getById(String uuid) {
        return organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    public Organization getByIdInternal(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    @Transactional
    public Organization update(String uuid, String name, String billingContactEmail) {
        Organization org = getById(uuid);
        if (name != null) org.setName(name);
        if (billingContactEmail != null) org.setBillingContactEmail(billingContactEmail);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization updatePolicies(String uuid, Map<String, Object> policyUpdates) throws JsonProcessingException {
        Organization org = getById(uuid);
        String merged = policyService.mergePolicies(org.getPolicies(), policyUpdates);
        org.setPolicies(merged);
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization deactivate(String uuid) {
        Organization org = getById(uuid);
        org.setStatus(Organization.OrganizationStatus.DEACTIVATED);
        return organizationRepository.save(org);
    }

    private void seedDefaultRoles(Organization org) {
        // Viewer role - base permissions
        List<Permission> viewerPerms = permissionRepository.findAll().stream()
                .filter(p -> p.getName().startsWith("view-"))
                .toList();
        Role viewer = Role.builder()
                .organization(org)
                .name("Viewer")
                .description("Can view workspaces, users, roles, and groups")
                .system(true)
                .permissions(new HashSet<>(viewerPerms))
                .build();
        viewer = roleRepository.save(viewer);

        // Editor role - inherits Viewer
        Role editor = Role.builder()
                .organization(org)
                .name("Editor")
                .description("Inherits all Viewer permissions")
                .parentRole(viewer)
                .system(true)
                .build();
        editor = roleRepository.save(editor);

        // Admin role - inherits Editor + manage permissions
        List<Permission> adminPerms = permissionRepository.findAll().stream()
                .filter(p -> p.getName().startsWith("manage-") || p.getName().equals("view-audit-log"))
                .toList();
        Role admin = Role.builder()
                .organization(org)
                .name("Admin")
                .description("Full administrative access")
                .parentRole(editor)
                .system(true)
                .permissions(new HashSet<>(adminPerms))
                .build();
        roleRepository.save(admin);
    }
}
