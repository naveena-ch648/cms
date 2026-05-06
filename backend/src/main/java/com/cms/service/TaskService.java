package com.cms.service;

import com.cms.entity.FileEntity;
import com.cms.entity.Notification;
import com.cms.entity.Task;
import com.cms.entity.User;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FileRepository;
import com.cms.repository.TaskRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Page<Task> getFileTasks(String fileUuid, int page, int size) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        return taskRepository.findByFileIdOrderByCreatedAtDesc(file.getId(), PageRequest.of(page, size));
    }

    public Page<Task> getMyTasks(Long userId, int page, int size) {
        return taskRepository.findByAssigneeIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional
    public Task createTask(String fileUuid, User creator, String title, String description, LocalDate dueDate, String assigneeUuid) {
        if (title == null || title.isBlank() || title.length() > 255) {
            throw new IllegalArgumentException("Task title must be 1-255 characters");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Task description must be at most 2000 characters");
        }

        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        User assignee = creator; // default to self-assign
        if (assigneeUuid != null && !assigneeUuid.isBlank()) {
            assignee = userRepository.findByUuid(assigneeUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Task task = Task.builder()
                .uuid(UUID.randomUUID().toString())
                .file(file)
                .creator(creator)
                .assignee(assignee)
                .title(title)
                .description(description)
                .dueDate(dueDate)
                .status(Task.Status.OPEN)
                .build();

        task = taskRepository.save(task);

        // Notify assignee if different from creator
        if (!assignee.getId().equals(creator.getId())) {
            notificationService.createNotification(
                    assignee,
                    Notification.Type.TASK_ASSIGNED,
                    "Task assigned to you",
                    "\"" + title + "\" was assigned to you by " + creator.getFirstName() + " " + creator.getLastName(),
                    "file",
                    file.getUuid(),
                    creator
            );
        }

        return task;
    }

    @Transactional
    public Task updateTask(String taskUuid, User actor, String status, String title, String description, LocalDate dueDate) {
        if (title != null && (title.isBlank() || title.length() > 255)) {
            throw new IllegalArgumentException("Task title must be 1-255 characters");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Task description must be at most 2000 characters");
        }

        Task task = taskRepository.findByUuid(taskUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (status != null) {
            Task.Status newStatus = Task.Status.valueOf(status);
            if (newStatus == Task.Status.DONE && task.getStatus() == Task.Status.OPEN) {
                task.setCompletedAt(Instant.now());
                // Notify creator if completed by someone else
                if (!actor.getId().equals(task.getCreator().getId())) {
                    notificationService.createNotification(
                            task.getCreator(),
                            Notification.Type.TASK_COMPLETED,
                            "Task completed",
                            "\"" + task.getTitle() + "\" was completed by " + actor.getFirstName() + " " + actor.getLastName(),
                            "file",
                            task.getFile().getUuid(),
                            actor
                    );
                }
            } else if (newStatus == Task.Status.OPEN) {
                task.setCompletedAt(null);
            }
            task.setStatus(newStatus);
        }
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (dueDate != null) task.setDueDate(dueDate);

        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(String taskUuid) {
        Task task = taskRepository.findByUuid(taskUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        taskRepository.delete(task);
    }
}
