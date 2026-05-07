package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.workflow.TriggerCreateRequest;
import com.cms.dto.workflow.TriggerResponse;
import com.cms.entity.User;
import com.cms.security.UserPrincipal;
import com.cms.service.TriggerService;
import com.cms.service.WorkspaceService;
import com.cms.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/workflow-triggers")
@RequiredArgsConstructor
public class TriggerController {

    private final TriggerService triggerService;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<TriggerResponse>> create(
            @PathVariable String workspaceId,
            @Valid @RequestBody TriggerCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = workspaceService.getByUuid(workspaceId).getId();
        User creator = userRepository.findById(principal.getId()).orElseThrow();
        TriggerResponse response = triggerService.create(wsId, request, creator);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TriggerResponse>>> list(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = workspaceService.getByUuid(workspaceId).getId();
        List<TriggerResponse> triggers = triggerService.listByWorkspace(wsId);
        return ResponseEntity.ok(ApiResponse.ok(triggers));
    }

    @PutMapping("/{triggerId}")
    public ResponseEntity<ApiResponse<TriggerResponse>> update(
            @PathVariable String workspaceId,
            @PathVariable String triggerId,
            @Valid @RequestBody TriggerCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TriggerResponse response = triggerService.update(triggerId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{triggerId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String workspaceId,
            @PathVariable String triggerId,
            @AuthenticationPrincipal UserPrincipal principal) {

        triggerService.delete(triggerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/{triggerId}")
    public ResponseEntity<ApiResponse<TriggerResponse>> toggle(
            @PathVariable String workspaceId,
            @PathVariable String triggerId,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean enabled = body.getOrDefault("enabled", true);
        TriggerResponse response = triggerService.toggle(triggerId, enabled);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
