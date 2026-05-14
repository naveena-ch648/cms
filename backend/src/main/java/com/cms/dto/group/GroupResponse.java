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
    private int memberCount;
    private Instant createdAt;

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .id(group.getUuid())
                .name(group.getName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .build();
    }

    public static GroupResponse from(Group group, int memberCount) {
        return GroupResponse.builder()
                .id(group.getUuid())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(memberCount)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
