package com.cms.dto.qa;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageDto {
    private String id;
    private String role;
    private String content;
    private List<CitationDto> citations;
    private String modelUsed;
    private int tokenCount;
    private String createdAt;
}
