package com.cms.service;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.UserOrganizationRole;
import com.cms.repository.PermissionRepository;
import com.cms.repository.UserOrganizationRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock UserOrganizationRoleRepository userOrgRoleRepository;
    @Mock PermissionRepository permissionRepository;

    @InjectMocks PermissionService permissionService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getEffectivePermissions_withNoOrgRole_returnsEmpty() {
        when(userOrgRoleRepository.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.empty());

        Set<String> perms = permissionService.getEffectivePermissions(1L, 1L);

        assertThat(perms).isEmpty();
    }

    @Test
    void getEffectivePermissions_withRole_returnsRolePermissions() {
        Permission p1 = new Permission(); p1.setName("FILE_READ");
        Permission p2 = new Permission(); p2.setName("FILE_WRITE");

        Role role = new Role();
        role.setId(1L);
        role.setName("EDITOR");
        role.setPermissions(Set.of(p1, p2));
        role.setParentRole(null);

        UserOrganizationRole orgRole = new UserOrganizationRole();
        orgRole.setRole(role);

        when(userOrgRoleRepository.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(orgRole));

        Set<String> perms = permissionService.getEffectivePermissions(1L, 1L);

        assertThat(perms).containsExactlyInAnyOrder("FILE_READ", "FILE_WRITE");
    }

    @Test
    void getEffectivePermissions_withParentRole_includesInheritedPermissions() {
        Permission parentPerm = new Permission(); parentPerm.setName("ADMIN_ACCESS");
        Role parentRole = new Role();
        parentRole.setId(2L);
        parentRole.setName("ADMIN");
        parentRole.setPermissions(Set.of(parentPerm));
        parentRole.setParentRole(null);

        Permission childPerm = new Permission(); childPerm.setName("FILE_READ");
        Role childRole = new Role();
        childRole.setId(3L);
        childRole.setName("VIEWER");
        childRole.setPermissions(Set.of(childPerm));
        childRole.setParentRole(parentRole);

        UserOrganizationRole orgRole = new UserOrganizationRole();
        orgRole.setRole(childRole);

        when(userOrgRoleRepository.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(orgRole));

        Set<String> perms = permissionService.getEffectivePermissions(1L, 1L);

        assertThat(perms).containsExactlyInAnyOrder("FILE_READ", "ADMIN_ACCESS");
    }

    @Test
    void hasPermission_withMatchingPermission_returnsTrue() {
        Permission p = new Permission(); p.setName("FILE_DELETE");
        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");
        role.setPermissions(Set.of(p));
        role.setParentRole(null);

        UserOrganizationRole orgRole = new UserOrganizationRole();
        orgRole.setRole(role);

        when(userOrgRoleRepository.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(orgRole));

        assertThat(permissionService.hasPermission(1L, 1L, "FILE_DELETE")).isTrue();
        assertThat(permissionService.hasPermission(1L, 1L, "BILLING_MANAGE")).isFalse();
    }

    @Test
    void invalidateCache_isNoOp() {
        permissionService.invalidateCache(1L, 1L);
        // Should not throw or do anything
    }
}
