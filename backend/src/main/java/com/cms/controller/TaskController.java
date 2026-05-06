package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.collaboration.TaskDto;
import com.cms.entity.Task;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    @GetMapping("/api/v1/files/{fileId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getFileTasks(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<Task> tasks = taskService.getFileTasks(fileId, page, size);
        List<TaskDto> dtos = tasks.getContent().stream().map(TaskDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/api/v1/files/{fileId}/tasks")
    public ResponseEntity<ApiResponse<TaskDto>> createTask(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String title = body.get("title");
        String description = body.get("description");
        LocalDate dueDate = body.get("dueDate") != null && !body.get("dueDate").isBlank()
                ? LocalDate.parse(body.get("dueDate")) : null;
        String assigneeId = body.get("assigneeId");

        Task task = taskService.createTask(fileId, creator, title, description, dueDate, assigneeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(TaskDto.from(task)));
    }

    @PatchMapping("/api/v1/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(
            @PathVariable String taskId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String status = body.get("status");
        String title = body.get("title");
        String description = body.get("description");
        LocalDate dueDate = body.get("dueDate") != null && !body.get("dueDate").isBlank()
                ? LocalDate.parse(body.get("dueDate")) : null;

        Task task = taskService.updateTask(taskId, actor, status, title, description, dueDate);
        return ResponseEntity.ok(ApiResponse.ok(TaskDto.from(task)));
    }

    @DeleteMapping("/api/v1/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/api/v1/tasks/my")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getMyTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<Task> tasks = taskService.getMyTasks(principal.getId(), page, size);
        List<TaskDto> dtos = tasks.getContent().stream().map(TaskDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }
}
