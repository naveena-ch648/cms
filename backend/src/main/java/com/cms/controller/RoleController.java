package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.role.*;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.middleware.TenantContext;
import com.cms.service.OrganizationService;
import com.cms.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> list(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<Role> page = roleService.list(orgId, pageable);
        List<RoleResponse> roles = page.getContent().stream()
                .map(RoleResponse::from).toList();
        ApiResponse.PagedMeta meta = ApiResponse.PagedMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(roles, meta));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable String roleId) {
        Role role = roleService.getById(roleId);
        return ResponseEntity.ok(ApiResponse.ok(RoleResponse.from(role)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'manage-roles')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(
            @Valid @RequestBody CreateRoleRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        var org = organizationService.getByIdInternal(orgId);
        Role role = roleService.create(orgId, request.getName(), request.getDescription(),
                request.getParentRoleId(), request.getPermissionIds(), org);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(RoleResponse.from(role)));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasPermission(null, 'manage-roles')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable String roleId, @Valid @RequestBody UpdateRoleRequest request) {
        Role role = roleService.update(roleId, request.getName(), request.getDescription(),
                request.getParentRoleId(), request.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.ok(RoleResponse.from(role)));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasPermission(null, 'manage-roles')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String roleId) {
        roleService.delete(roleId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<RoleResponse.PermissionInfo>>> listPermissions() {
        List<Permission> perms = roleService.listPermissions();
        List<RoleResponse.PermissionInfo> result = perms.stream()
                .map(p -> RoleResponse.PermissionInfo.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .category(p.getCategory())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
