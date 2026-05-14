package com.cms.config;

import com.cms.entity.*;
import com.cms.entity.Organization.OrganizationStatus;
import com.cms.entity.User.UserStatus;
import com.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL    = "mypatsiuman2018@gmail.com";
    private static final String ADMIN_PASSWORD = "Suman@2001";
    private static final String ORG_SLUG       = "default-org";

    private final OrganizationRepository  organizationRepository;
    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final PermissionRepository    permissionRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final PasswordEncoder         passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizationRepository.existsBySlug(ORG_SLUG)) {
            log.info("Default organization already exists — skipping data initialization.");
            return;
        }

        log.info("Initializing default organization, roles, permissions, and admin user...");

        // 1. Seed permissions
        List<String[]> permDefs = List.of(
            new String[]{"view-workspace",  "workspace", "View workspace and its contents"},
            new String[]{"manage-workspace","workspace", "Create, update, delete workspaces"},
            new String[]{"view-users",      "user",      "View user list"},
            new String[]{"manage-users",    "user",      "Create, update, deactivate users"},
            new String[]{"view-roles",      "role",      "View role definitions"},
            new String[]{"manage-roles",    "role",      "Create, update, delete roles"},
            new String[]{"view-groups",     "group",     "View groups"},
            new String[]{"manage-groups",   "group",     "Create, update, delete groups"},
            new String[]{"manage-policies", "organization", "Configure organization policies"},
            new String[]{"view-audit-log",  "audit",     "View audit events"},
            new String[]{"FILE_UPLOAD",     "file",      "Upload files to a folder"},
            new String[]{"FILE_DOWNLOAD",   "file",      "Download/preview files"},
            new String[]{"FILE_MANAGE",     "file",      "Rename, move, copy, delete files"},
            new String[]{"FILE_TRASH_RESTORE","file",    "Restore files from trash"},
            new String[]{"FILE_TRASH_DELETE","file",     "Permanently delete files from trash"}
        );

        for (String[] def : permDefs) {
            if (!permissionRepository.existsByName(def[0])) {
                permissionRepository.save(Permission.builder()
                    .name(def[0]).category(def[1]).description(def[2]).build());
            }
        }

        // 2. Create organization
        Organization org = Organization.builder()
            .name("Default Organization")
            .slug(ORG_SLUG)
            .billingContactEmail(ADMIN_EMAIL)
            .status(OrganizationStatus.ACTIVE)
            .policies("{\"passwordMinLength\":8,\"passwordRequireUppercase\":true,\"passwordRequireNumber\":true,\"passwordRequireSpecialChar\":false,\"sessionTimeoutMinutes\":30,\"maxWorkspaces\":50,\"maxFailedLoginAttempts\":5,\"accountLockoutMinutes\":15}")
            .build();
        org = organizationRepository.save(org);

        // 3. Seed roles
        List<Permission> viewPerms  = permissionRepository.findAll().stream()
            .filter(p -> p.getName().startsWith("view-")).toList();
        List<Permission> managePerms = permissionRepository.findAll().stream()
            .filter(p -> p.getName().startsWith("manage-") || p.getName().equals("view-audit-log")).toList();
        List<Permission> filePerms  = permissionRepository.findAll().stream()
            .filter(p -> p.getName().startsWith("FILE_")).toList();

        Role viewer = roleRepository.save(Role.builder()
            .organization(org).name("Viewer").description("Can view workspaces, users, roles, and groups")
            .system(true).permissions(new HashSet<>(viewPerms)).build());

        Role editor = roleRepository.save(Role.builder()
            .organization(org).name("Editor").description("Inherits all Viewer permissions")
            .parentRole(viewer).system(true)
            .permissions(new HashSet<>(filePerms)).build());

        List<Permission> allAdminPerms = new java.util.ArrayList<>(managePerms);
        allAdminPerms.addAll(filePerms);
        Role admin = roleRepository.save(Role.builder()
            .organization(org).name("Admin").description("Full administrative access")
            .parentRole(editor).system(true)
            .permissions(new HashSet<>(allAdminPerms)).build());

        // 4. Create admin user
        User adminUser = User.builder()
            .organization(org)
            .email(ADMIN_EMAIL)
            .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
            .firstName("Suman")
            .lastName("Mypati")
            .status(UserStatus.ACTIVE)
            .build();
        adminUser = userRepository.save(adminUser);

        // 5. Assign Admin role
        userOrgRoleRepository.save(UserOrganizationRole.builder()
            .userId(adminUser.getId())
            .organizationId(org.getId())
            .role(admin)
            .build());

        log.info("✓ Default admin created: email={}, organizationId={}", ADMIN_EMAIL, org.getId());
    }
}
