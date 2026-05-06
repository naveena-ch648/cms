package com.cms.service;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PermissionRepository;
import com.cms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public Role getById(String uuid) {
        return roleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    public Page<Role> list(Long organizationId, Pageable pageable) {
        return roleRepository.findByOrganizationId(organizationId, pageable);
    }

    @Transactional
    public Role create(Long organizationId, String name, String description,
                       String parentRoleUuid, List<Long> permissionIds,
                       com.cms.entity.Organization organization) {
        if (roleRepository.findByNameAndOrganizationId(name, organizationId).isPresent()) {
            throw new DuplicateResourceException("ROLE_NAME_EXISTS", "Role name already exists in this organization");
        }

        Role.RoleBuilder builder = Role.builder()
                .organization(organization)
                .name(name)
                .description(description)
                .system(false);

        if (parentRoleUuid != null) {
            Role parent = roleRepository.findByUuid(parentRoleUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent role not found"));
            builder.parentRole(parent);
        }

        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findByIdIn(permissionIds);
            builder.permissions(new HashSet<>(permissions));
        }

        return roleRepository.save(builder.build());
    }

    @Transactional
    public Role update(String uuid, String name, String description,
                       String parentRoleUuid, List<Long> permissionIds) {
        Role role = getById(uuid);

        if (role.isSystem()) {
            throw new BusinessRuleException("SYSTEM_ROLE", "Cannot modify system roles");
        }

        if (name != null) role.setName(name);
        if (description != null) role.setDescription(description);

        if (parentRoleUuid != null) {
            Role parent = roleRepository.findByUuid(parentRoleUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent role not found"));
            // Prevent circular references
            if (parent.getId().equals(role.getId())) {
                throw new BusinessRuleException("CIRCULAR_ROLE", "Role cannot be its own parent");
            }
            role.setParentRole(parent);
        }

        if (permissionIds != null) {
            List<Permission> permissions = permissionRepository.findByIdIn(permissionIds);
            role.setPermissions(new HashSet<>(permissions));
        }

        return roleRepository.save(role);
    }

    @Transactional
    public void delete(String uuid) {
        Role role = getById(uuid);
        if (role.isSystem()) {
            throw new BusinessRuleException("SYSTEM_ROLE", "Cannot delete system roles");
        }
        // Check for child roles
        List<Role> children = roleRepository.findByParentRoleId(role.getId());
        if (!children.isEmpty()) {
            throw new BusinessRuleException("ROLE_HAS_CHILDREN", "Cannot delete role with child roles");
        }
        roleRepository.delete(role);
    }

    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
}
