package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.group.*;
import com.cms.dto.user.UserResponse;
import com.cms.entity.Group;
import com.cms.entity.User;
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
        int count = (int) groupService.countMembers(group.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(GroupResponse.from(group, count)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> list(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<Group> page = groupService.list(orgId, pageable);
        List<GroupResponse> groups = page.getContent().stream()
                .map(g -> GroupResponse.from(g, (int) groupService.countMembers(g.getId())))
                .toList();
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
        int count = (int) groupService.countMembers(group.getId());
        return ResponseEntity.ok(ApiResponse.ok(GroupResponse.from(group, count)));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getMembers(@PathVariable String groupId) {
        List<User> users = groupService.getUsersInGroup(groupId);
        List<UserResponse> responses = users.stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasPermission(null, 'manage-groups')")
    public ResponseEntity<ApiResponse<GroupResponse>> update(
            @PathVariable String groupId, @Valid @RequestBody UpdateGroupRequest request) {
        Group group = groupService.update(groupId, request.getName(), request.getDescription());
        int count = (int) groupService.countMembers(group.getId());
        return ResponseEntity.ok(ApiResponse.ok(GroupResponse.from(group, count)));
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
            @PathVariable String groupId, @PathVariable String userId) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
