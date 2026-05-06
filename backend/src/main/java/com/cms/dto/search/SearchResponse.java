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
public class SearchResponse {

    private List<SearchResultDto> results;
    private Pagination pagination;
    private String query;
    private Map<String, Object> filters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int page;
        private int size;
        private long totalResults;
        private int totalPages;
    }
}
