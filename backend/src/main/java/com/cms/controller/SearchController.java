package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.search.AutocompleteResponse;
import com.cms.dto.search.SearchRequest;
import com.cms.dto.search.SearchResponse;
import com.cms.dto.search.SearchResultDto;
import com.cms.security.UserPrincipal;
import com.cms.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(required = true) String workspaceId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> fileType,
            @RequestParam(required = false) String ownerUuid,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "updatedAt") String dateField,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Validate query length
        if (q != null && q.length() > 500) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_QUERY", "Query must not exceed 500 characters"));
        }

        // Validate page/size bounds
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        // Validate sortBy
        List<String> validSortFields = List.of("relevance", "name", "dateModified", "dateCreated", "fileSize", "owner");
        if (!validSortFields.contains(sortBy)) {
            sortBy = "relevance";
        }

        // Validate sortOrder
        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            sortOrder = "desc";
        }

        SearchRequest request = SearchRequest.builder()
                .query(q)
                .workspaceId(workspaceId)
                .fileType(fileType)
                .ownerUuid(ownerUuid)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .dateField(dateField)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .build();

        SearchResponse response = searchService.search(request);

        // Save to recent searches if query is non-empty
        if (q != null && !q.isBlank()) {
            searchService.saveRecentSearch(principal.getId(), q.trim());
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/search/autocomplete")
    public ResponseEntity<ApiResponse<AutocompleteResponse>> autocomplete(
            @RequestParam(required = true) String q,
            @RequestParam(required = true) String workspaceId,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (limit > 10) limit = 10;

        List<SearchResultDto> files = List.of();
        if (q.length() >= 2) {
            files = searchService.autocomplete(q, workspaceId, limit);
        }

        List<String> recentSearches = searchService.getRecentSearches(principal.getId(), q);

        AutocompleteResponse response = AutocompleteResponse.builder()
                .files(files)
                .recentSearches(recentSearches)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/search/recent")
    public ResponseEntity<ApiResponse<Void>> saveRecentSearch(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        String query = body.get("query");
        if (query != null && !query.isBlank()) {
            searchService.saveRecentSearch(principal.getId(), query.trim());
        }

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/search/recent")
    public ResponseEntity<ApiResponse<Void>> clearRecentSearches(
            @AuthenticationPrincipal UserPrincipal principal) {

        searchService.clearRecentSearches(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/admin/search/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex(
            @RequestParam(required = true) String workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        long count = searchService.triggerReindex(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "workspaceId", workspaceId,
                "filesQueued", count
        )));
    }

    @GetMapping("/search/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = searchService.getHealthInfo();
        return ResponseEntity.ok(ApiResponse.ok(healthInfo));
    }
}
