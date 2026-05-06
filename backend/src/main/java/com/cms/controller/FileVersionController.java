package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.file.FileVersionDto;
import com.cms.entity.FileEntity;
import com.cms.entity.FileVersion;
import com.cms.entity.User;
import com.cms.service.FileVersionService;
import com.cms.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/files/{fileId}/versions")
@RequiredArgsConstructor
public class FileVersionController {

    private final FileVersionService fileVersionService;
    private final FileRepository fileRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<FileVersionDto>> uploadVersion(
            @PathVariable String fileId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeNote", required = false) String changeNote,
            @AuthenticationPrincipal User user) {

        FileVersion version = fileVersionService.uploadNewVersion(fileId, file, changeNote, user);
        FileEntity fileEntity = version.getFile();
        boolean isCurrent = fileEntity.getCurrentVersion() != null
                && fileEntity.getCurrentVersion().getId().equals(version.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(FileVersionDto.from(version, true)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileVersionDto>>> listVersions(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<FileVersion> versions = fileVersionService.listVersions(fileId, pageable);

        FileEntity fileEntity = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        Long currentVersionId = fileEntity.getCurrentVersion() != null
                ? fileEntity.getCurrentVersion().getId() : null;

        Page<FileVersionDto> dtos = versions.map(v ->
                FileVersionDto.from(v, v.getId().equals(currentVersionId)));

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<ApiResponse<FileVersionDto>> getVersion(
            @PathVariable String fileId,
            @PathVariable String versionId) {

        FileVersion version = fileVersionService.getVersion(fileId, versionId);
        FileEntity fileEntity = version.getFile();
        boolean isCurrent = fileEntity.getCurrentVersion() != null
                && fileEntity.getCurrentVersion().getId().equals(version.getId());

        return ResponseEntity.ok(ApiResponse.ok(FileVersionDto.from(version, isCurrent)));
    }

    @GetMapping("/{versionId}/download")
    public ResponseEntity<Void> downloadVersion(
            @PathVariable String fileId,
            @PathVariable String versionId) {

        String url = fileVersionService.getVersionDownloadUrl(fileId, versionId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @PostMapping("/{versionId}/restore")
    public ResponseEntity<ApiResponse<FileVersionDto>> restoreVersion(
            @PathVariable String fileId,
            @PathVariable String versionId,
            @AuthenticationPrincipal User user) {

        FileVersion restored = fileVersionService.restoreVersion(fileId, versionId, user);
        return ResponseEntity.ok(ApiResponse.ok(FileVersionDto.from(restored, true)));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareVersions(
            @PathVariable String fileId,
            @RequestParam("v1") String versionId1,
            @RequestParam("v2") String versionId2) {

        Map<String, Object> comparison = fileVersionService.compareVersions(fileId, versionId1, versionId2);
        return ResponseEntity.ok(ApiResponse.ok(comparison));
    }
}
