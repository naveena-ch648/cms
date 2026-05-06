package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.preview.PreviewDto;
import com.cms.dto.preview.PreviewJobDto;
import com.cms.dto.preview.ThumbnailDto;
import com.cms.entity.FileEntity;
import com.cms.entity.PreviewJob;
import com.cms.repository.FileRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.PreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files/{fileId}")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;
    private final FileRepository fileRepository;

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewDto>> getPreview(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        PreviewDto preview = previewService.getOrTriggerPreview(file);
        return ResponseEntity.ok(ApiResponse.ok(preview));
    }

    @GetMapping("/thumbnail")
    public ResponseEntity<ApiResponse<ThumbnailDto>> getThumbnail(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        ThumbnailDto thumbnail = previewService.getThumbnail(file);
        if (thumbnail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(thumbnail));
    }

    @PostMapping("/preview/regenerate")
    public ResponseEntity<ApiResponse<PreviewJobDto>> regeneratePreview(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        PreviewJob job = previewService.regeneratePreview(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(PreviewJobDto.from(job)));
    }

    @GetMapping("/preview/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreviewStatus(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        Map<String, Object> status = previewService.getPreviewStatus(file);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
