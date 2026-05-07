package com.cms.dto.qa;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SummarizeResponse {
    private String documentId;
    private String documentName;
    private String summary;
    private List<CitationDto> citations;
    private String modelUsed;
    private int tokenCount;
}
