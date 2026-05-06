package com.cms.dto.group;

import com.cms.entity.Group;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {

    private String id;
    private String name;
    private String description;
    private Instant createdAt;

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .id(group.getUuid())
                .name(group.getName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
