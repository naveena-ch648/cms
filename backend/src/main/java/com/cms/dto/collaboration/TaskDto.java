package com.cms.dto.collaboration;

import com.cms.entity.Task;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {

    private String id;
    private String fileId;
    private String title;
    private String description;
    private String status;
    private LocalDate dueDate;
    private boolean overdue;
    private ActorDto creator;
    private ActorDto assignee;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActorDto {
        private String id;
        private String name;
        private String email;
    }

    public static TaskDto from(Task task) {
        boolean isOverdue = task.getStatus() == Task.Status.OPEN
                && task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now());

        return TaskDto.builder()
                .id(task.getUuid())
                .fileId(task.getFile() != null ? task.getFile().getUuid() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .dueDate(task.getDueDate())
                .overdue(isOverdue)
                .creator(task.getCreator() != null ? ActorDto.builder()
                        .id(task.getCreator().getUuid())
                        .name(task.getCreator().getFirstName() + " " + task.getCreator().getLastName())
                        .email(task.getCreator().getEmail())
                        .build() : null)
                .assignee(task.getAssignee() != null ? ActorDto.builder()
                        .id(task.getAssignee().getUuid())
                        .name(task.getAssignee().getFirstName() + " " + task.getAssignee().getLastName())
                        .email(task.getAssignee().getEmail())
                        .build() : null)
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
