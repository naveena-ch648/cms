package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.group.*;
import com.cms.entity.Group;
import com.cms.middleware.TenantContext;
import com.cms.service.GroupService;
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
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<GroupResponse>> create(
            @Valid @RequestBody CreateGroupRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Group group = groupService.create(orgId, request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(GroupResponse.from(group)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> list(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<Group> page = groupService.list(orgId, pageable);
        List<GroupResponse> groups = page.getContent().stream()
                .map(GroupResponse::from).toList();
        ApiResponse.PagedMeta meta = ApiResponse.PagedMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(groups, meta));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getById(@PathVariable String groupId) {
        Group group = groupService.getById(groupId);
        return ResponseEntity.ok(ApiResponse.ok(GroupResponse.from(group)));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<GroupResponse>> update(
            @PathVariable String groupId, @Valid @RequestBody UpdateGroupRequest request) {
        Group group = groupService.update(groupId, request.getName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok(GroupResponse.from(group)));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String groupId) {
        groupService.delete(groupId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable String groupId, @Valid @RequestBody AddGroupMemberRequest request) {
        groupService.addMember(groupId, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable String groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
