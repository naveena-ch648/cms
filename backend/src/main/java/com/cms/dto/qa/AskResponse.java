package com.cms.dto.qa;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AskResponse {

    private String conversationId;
    private String messageId;
    private String answer;
    private List<CitationDto> citations;
    private String modelUsed;
    private Integer tokenCount;
    private boolean noRelevantInfo;
}
