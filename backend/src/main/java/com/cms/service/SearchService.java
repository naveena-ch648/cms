package com.cms.service;

import com.cms.dto.search.SearchRequest;
import com.cms.dto.search.SearchResponse;
import com.cms.dto.search.SearchResultDto;
import com.cms.event.FileIndexEventPublisher;
import com.cms.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final OpenSearchClient openSearchClient;
    private final FileRepository fileRepository;
    private final FileIndexEventPublisher fileIndexEventPublisher;

    @Value("${opensearch.index-name}")
    private String indexName;

    private static final String RECENT_SEARCHES_KEY_PREFIX = "search:recent:";
    private static final int MAX_RECENT_SEARCHES = 20;

    public SearchResponse search(SearchRequest request) {
        try {
            var osResponse = openSearchClient.search(s -> s
                            .index(indexName)
                            .query(buildQuery(request))
                            .highlight(buildHighlight())
                            .from(request.getPage() * request.getSize())
                            .size(request.getSize())
                            .sort(buildSort(request)),
                    Map.class
            );

            List<SearchResultDto> results = mapResults(osResponse);
            long totalHits = osResponse.hits().total() != null ? osResponse.hits().total().value() : 0;

            return SearchResponse.builder()
                    .results(results)
                    .pagination(SearchResponse.Pagination.builder()
                            .page(request.getPage())
                            .size(request.getSize())
                            .totalResults(totalHits)
                            .totalPages((int) Math.ceil((double) totalHits / request.getSize()))
                            .build())
                    .query(request.getQuery())
                    .filters(buildFiltersSummary(request))
                    .build();
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            throw new SearchUnavailableException("Search service is temporarily unavailable");
        }
    }

    public List<SearchResultDto> autocomplete(String prefix, String workspaceId, int limit) {
        try {
            var osResponse = openSearchClient.search(s -> s
                            .index(indexName)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.matchPhrasePrefix(mp -> mp
                                            .field("fileName")
                                            .query(prefix)
                                    ))
                                    .filter(f -> f.term(t -> t
                                            .field("workspaceUuid")
                                            .value(FieldValue.of(workspaceId))
                                    ))
                            ))
                            .size(limit)
                            .source(src -> src.filter(sf -> sf
                                    .includes("fileUuid", "fileName", "folderPath", "fileType")
                            )),
                    Map.class
            );

            return osResponse.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = hit.source();
                        if (source == null) return null;
                        return SearchResultDto.builder()
                                .fileUuid((String) source.get("fileUuid"))
                                .fileName((String) source.get("fileName"))
                                .folderPath((String) source.get("folderPath"))
                                .fileType((String) source.get("fileType"))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Autocomplete failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public void saveRecentSearch(Long userId, String query) {
        // Recent searches stored in-memory only (no Redis)
    }

    public List<String> getRecentSearches(Long userId, String prefix) {
        return Collections.emptyList();
    }

    public void clearRecentSearches(Long userId) {
        // No-op
    }

    private Query buildQuery(SearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Mandatory workspace filter
        boolBuilder.filter(f -> f.term(t -> t
                .field("workspaceUuid")
                .value(FieldValue.of(request.getWorkspaceId()))
        ));

        // Keyword search (if provided)
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(request.getQuery())
                    .fields("fileName^3", "content", "ownerName^2", "folderPath")
            ));
        }

        // File type filter
        if (request.getFileType() != null && !request.getFileType().isEmpty()) {
            List<FieldValue> typeValues = request.getFileType().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            boolBuilder.filter(f -> f.terms(t -> t
                    .field("fileType")
                    .terms(tv -> tv.value(typeValues))
            ));
        }

        // Owner filter
        if (request.getOwnerUuid() != null && !request.getOwnerUuid().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("ownerUuid")
                    .value(FieldValue.of(request.getOwnerUuid()))
            ));
        }

        // Date range filter
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            String dateField = "updatedAt".equals(request.getDateField()) ? "updatedAt" : "createdAt";
            boolBuilder.filter(f -> f.range(r -> {
                var rangeBuilder = r.field(dateField);
                if (request.getDateFrom() != null) {
                    rangeBuilder.gte(org.opensearch.client.json.JsonData.of(request.getDateFrom()));
                }
                if (request.getDateTo() != null) {
                    rangeBuilder.lte(org.opensearch.client.json.JsonData.of(request.getDateTo()));
                }
                return rangeBuilder;
            }));
        }

        // Tag filters (AND logic)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                boolBuilder.filter(f -> f.term(t -> t
                        .field("tags")
                        .value(FieldValue.of(tag.toLowerCase()))
                ));
            }
        }

        // Metadata filters
        if (request.getMetadataFilters() != null && !request.getMetadataFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getMetadataFilters().entrySet()) {
                String fieldKey = entry.getKey();
                String fieldValue = entry.getValue();
                String metaField = "metadata." + fieldKey.toLowerCase().replaceAll("[^a-z0-9_]", "_");

                if (fieldKey.endsWith(".gte")) {
                    String actualField = "metadata." + fieldKey.replace(".gte", "").toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    boolBuilder.filter(f -> f.range(r -> r
                            .field(actualField)
                            .gte(org.opensearch.client.json.JsonData.of(fieldValue))
                    ));
                } else if (fieldKey.endsWith(".lte")) {
                    String actualField = "metadata." + fieldKey.replace(".lte", "").toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    boolBuilder.filter(f -> f.range(r -> r
                            .field(actualField)
                            .lte(org.opensearch.client.json.JsonData.of(fieldValue))
                    ));
                } else {
                    boolBuilder.filter(f -> f.term(t -> t
                            .field(metaField)
                            .value(FieldValue.of(fieldValue))
                    ));
                }
            }
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    private Highlight buildHighlight() {
        return Highlight.of(h -> h
                .preTags("<mark>")
                .postTags("</mark>")
                .fields("content", HighlightField.of(hf -> hf
                        .fragmentSize(150)
                        .numberOfFragments(3)
                ))
                .fields("fileName", HighlightField.of(hf -> hf
                        .numberOfFragments(0)
                ))
        );
    }

    private List<org.opensearch.client.opensearch._types.SortOptions> buildSort(SearchRequest request) {
        if ("relevance".equals(request.getSortBy()) ||
                (request.getQuery() != null && !request.getQuery().isBlank() && request.getSortBy() == null)) {
            return Collections.emptyList(); // Default to _score
        }

        String fieldName = switch (request.getSortBy()) {
            case "name" -> "fileName.keyword";
            case "dateModified" -> "updatedAt";
            case "dateCreated" -> "createdAt";
            case "fileSize" -> "fileSize";
            case "owner" -> "ownerName.keyword";
            default -> null;
        };

        if (fieldName == null) return Collections.emptyList();

        SortOrder order = "asc".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
        String sortField = fieldName;

        return List.of(org.opensearch.client.opensearch._types.SortOptions.of(so -> so
                .field(f -> f.field(sortField).order(order))
        ));
    }

    @SuppressWarnings("unchecked")
    private List<SearchResultDto> mapResults(org.opensearch.client.opensearch.core.SearchResponse<Map> osResponse) {
        return osResponse.hits().hits().stream()
                .map(hit -> {
                    Map<String, Object> source = hit.source();
                    if (source == null) return null;

                    List<String> highlights = new ArrayList<>();
                    if (hit.highlight() != null) {
                        hit.highlight().forEach((field, fragments) -> highlights.addAll(fragments));
                    }

                    return SearchResultDto.builder()
                            .fileUuid((String) source.get("fileUuid"))
                            .fileName((String) source.get("fileName"))
                            .fileType((String) source.get("fileType"))
                            .mimeType((String) source.get("mimeType"))
                            .fileSize(source.get("fileSize") != null ? ((Number) source.get("fileSize")).longValue() : null)
                            .ownerUuid((String) source.get("ownerUuid"))
                            .ownerName((String) source.get("ownerName"))
                            .folderPath((String) source.get("folderPath"))
                            .folderUuid((String) source.get("folderUuid"))
                            .createdAt((String) source.get("createdAt"))
                            .updatedAt((String) source.get("updatedAt"))
                            .highlights(highlights.isEmpty() ? null : highlights)
                            .score(hit.score())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildFiltersSummary(SearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("fileType", request.getFileType());
        filters.put("ownerUuid", request.getOwnerUuid());
        filters.put("dateFrom", request.getDateFrom());
        filters.put("dateTo", request.getDateTo());
        return filters;
    }

    public long triggerReindex(String workspaceId) {
        var files = fileRepository.findUuidsByWorkspaceUuid(workspaceId);
        for (String fileUuid : files) {
            fileIndexEventPublisher.publishIndexEvent(fileUuid, workspaceId, null);
        }
        return files.size();
    }

    public Map<String, Object> getHealthInfo() {
        try {
            var clusterHealth = openSearchClient.cluster().health();
            var indexStats = openSearchClient.indices().stats(s -> s.index(indexName));

            Map<String, Object> info = new HashMap<>();
            info.put("status", clusterHealth.status().jsonValue());
            info.put("indexName", indexName);
            info.put("documentCount", indexStats.indices().get(indexName) != null
                    ? indexStats.indices().get(indexName).primaries().docs().count() : 0);
            info.put("indexSize", indexStats.indices().get(indexName) != null
                    ? indexStats.indices().get(indexName).primaries().store().size() : "0b");
            return info;
        } catch (Exception e) {
            log.error("Failed to get search health: {}", e.getMessage());
            Map<String, Object> info = new HashMap<>();
            info.put("status", "unavailable");
            info.put("error", e.getMessage());
            return info;
        }
    }

    public static class SearchUnavailableException extends RuntimeException {
        public SearchUnavailableException(String message) {
            super(message);
        }
    }
}
