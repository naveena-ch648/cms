package com.cms.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String query;
    private String workspaceId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private List<String> fileType;
    private String ownerUuid;
    private String dateFrom;
    private String dateTo;

    @Builder.Default
    private String dateField = "updatedAt";

    @Builder.Default
    private String sortBy = "relevance";

    @Builder.Default
    private String sortOrder = "desc";

    private List<String> tags;
    private Map<String, String> metadataFilters;
}
