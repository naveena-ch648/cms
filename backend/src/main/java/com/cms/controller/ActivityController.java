package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.collaboration.ActivityEventDto;
import com.cms.entity.AuditEvent;
import com.cms.entity.FileEntity;
import com.cms.entity.Folder;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.AuditEventRepository;
import com.cms.repository.FileRepository;
import com.cms.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final AuditEventRepository auditEventRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @GetMapping("/api/v1/files/{fileId}/activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileActivity(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Page<AuditEvent> events = auditEventRepository
                .findByResourceTypeAndResourceIdOrderByCreatedAtDesc("FILE", file.getId(), PageRequest.of(page, size));

        List<ActivityEventDto> dtos = events.getContent().stream()
                .map(ActivityEventDto::from).toList();

        Map<String, Object> result = Map.of(
                "content", dtos,
                "number", events.getNumber(),
                "totalPages", events.getTotalPages(),
                "totalElements", events.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/api/v1/folders/{folderId}/activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFolderActivity(
            @PathVariable String folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Folder folder = folderRepository.findByUuid(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        Page<AuditEvent> events = auditEventRepository
                .findByResourceTypeAndResourceIdOrderByCreatedAtDesc("FOLDER", folder.getId(), PageRequest.of(page, size));

        List<ActivityEventDto> dtos = events.getContent().stream()
                .map(ActivityEventDto::from).toList();

        Map<String, Object> result = Map.of(
                "content", dtos,
                "number", events.getNumber(),
                "totalPages", events.getTotalPages(),
                "totalElements", events.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
