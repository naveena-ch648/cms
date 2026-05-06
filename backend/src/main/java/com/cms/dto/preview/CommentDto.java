package com.cms.dto.preview;

import com.cms.entity.Comment;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private String id;
    private String content;
    private AuthorDto author;
    private String parentId;
    private List<CommentDto> replies;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorDto {
        private String id;
        private String name;
        private String email;
    }

    public static CommentDto from(Comment comment) {
        return CommentDto.builder()
                .id(comment.getUuid())
                .content(comment.getContent())
                .author(comment.getUser() != null ? AuthorDto.builder()
                        .id(comment.getUser().getUuid())
                        .name(comment.getUser().getFirstName() + " " + comment.getUser().getLastName())
                        .email(comment.getUser().getEmail())
                        .build() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getUuid() : null)
                .replies(comment.getReplies() != null ?
                        comment.getReplies().stream().map(CommentDto::from).collect(Collectors.toList()) :
                        List.of())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
