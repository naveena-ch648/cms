package com.cms.dto.metadata;

import com.cms.entity.FileTag;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {

    private String name;
    private Instant createdAt;
    private String createdBy;

    public static TagResponse from(FileTag tag) {
        return TagResponse.builder()
                .name(tag.getName())
                .createdAt(tag.getCreatedAt())
                .createdBy(tag.getCreatedBy() != null ?
                        tag.getCreatedBy().getFirstName() + " " + tag.getCreatedBy().getLastName() : null)
                .build();
    }
}
