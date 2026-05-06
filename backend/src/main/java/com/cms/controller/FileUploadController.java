package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.file.*;
import com.cms.entity.FileEntity;
import com.cms.entity.User;
import com.cms.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") String folderId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "onDuplicate", required = false, defaultValue = "rename") String onDuplicate,
            @AuthenticationPrincipal User user) throws IOException {

        FileEntity created = fileUploadService.uploadSingleFile(file, folderId, description, tags, onDuplicate, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FileDto.from(created)));
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<UploadInitiateResponse>> initiateChunkedUpload(
            @Valid @RequestBody UploadInitiateRequest request,
            @AuthenticationPrincipal User user) {

        UploadInitiateResponse response = fileUploadService.initiateChunkedUpload(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/{sessionId}/chunks/{chunkNumber}")
    public ResponseEntity<ApiResponse<ChunkUploadResponse>> uploadChunk(
            @PathVariable String sessionId,
            @PathVariable int chunkNumber,
            @RequestBody byte[] data) throws IOException {

        ChunkUploadResponse response = fileUploadService.uploadChunk(sessionId, chunkNumber, data);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<FileDto>> completeUpload(
            @PathVariable String sessionId,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String checksum = body != null ? body.get("checksumSha256") : null;
        FileEntity file = fileUploadService.completeChunkedUpload(sessionId, checksum, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FileDto.from(file)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> abortUpload(@PathVariable String sessionId) {
        fileUploadService.abortUpload(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<ApiResponse<UploadSessionStatusDto>> getStatus(@PathVariable String sessionId) {
        UploadSessionStatusDto status = fileUploadService.getSessionStatus(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
