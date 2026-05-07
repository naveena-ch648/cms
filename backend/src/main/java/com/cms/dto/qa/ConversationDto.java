package com.cms.dto.qa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationDto {
    private String id;
    private String title;
    private String status;
    private int messageCount;
    private String createdAt;
    private String updatedAt;
}
