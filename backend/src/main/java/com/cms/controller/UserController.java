package com.cms.controller;

import com.cms.annotation.Audited;
import com.cms.dto.ApiResponse;
import com.cms.dto.user.*;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;
import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.middleware.TenantContext;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userService.register(orgId, request.getEmail(), request.getFirstName(),
                request.getLastName(), request.getPassword(), request.getRoleId());
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), orgId).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserResponse.from(user, orgRole)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        User.UserStatus userStatus = status != null ? User.UserStatus.valueOf(status) : null;
        Page<User> page = userService.list(orgId, search, userStatus, pageable);

        List<UserResponse> users = page.getContent().stream()
                .map(u -> {
                    UserOrganizationRole orgRole = userOrgRoleRepository
                            .findByUserIdAndOrganizationId(u.getId(), orgId).orElse(null);
                    return UserResponse.from(u, orgRole);
                })
                .toList();

        ApiResponse.PagedMeta pagedMeta = ApiResponse.PagedMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(users, pagedMeta));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String userId) {
        User user = userService.getById(userId);
        Long orgId = user.getOrganization().getId();
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), orgId).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user, orgRole)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        User.UserStatus status = request.getStatus() != null
                ? User.UserStatus.valueOf(request.getStatus()) : null;
        User user = userService.update(userId, request.getFirstName(), request.getLastName(), status);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PutMapping("/{userId}/role")
    @Audited(event = AuditEventType.ROLE_ASSIGNED, category = AuditCategory.PERMISSION_CHANGE, resourceType = "user")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable String userId, @Valid @RequestBody ChangeRoleRequest request) {
        userService.changeRole(userId, request.getRoleId());
        User user = userService.getById(userId);
        Long orgId = user.getOrganization().getId();
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), orgId).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user, orgRole)));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable String userId, @Valid @RequestBody ChangePasswordRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        userService.changePassword(userId, request.getNewPassword(), orgId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable String userId) {
        User user = userService.deactivate(userId);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }
}
