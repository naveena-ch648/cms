package com.cms.dto.qa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CitationDto {

    private int index;
    private String documentId;
    private String documentName;
    private int pageNumber;
    private String excerpt;
    private String chunkId;
    private int charStart;
    private int charEnd;
}
