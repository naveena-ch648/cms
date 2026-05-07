package com.cms.service;

import com.cms.entity.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.*;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditSearchService {

    private final OpenSearchClient openSearchClient;
    private static final String INDEX_NAME = "audit_events";

    @PostConstruct
    public void initIndex() {
        try {
            boolean exists = openSearchClient.indices().exists(
                    ExistsRequest.of(e -> e.index(INDEX_NAME))
            ).value();

            if (!exists) {
                CreateIndexRequest request = new CreateIndexRequest.Builder()
                        .index(INDEX_NAME)
                        .settings(IndexSettings.of(s -> s
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                        ))
                        .mappings(TypeMapping.of(m -> m
                                .properties(Map.ofEntries(
                                        Map.entry("id", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                        Map.entry("organization_id", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                        Map.entry("user_id", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                        Map.entry("actor_name", Property.of(p -> p.text(TextProperty.of(t -> t
                                                .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
                                        )))),
                                        Map.entry("event_type", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                        Map.entry("category", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                        Map.entry("resource_type", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                        Map.entry("resource_id", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                        Map.entry("resource_name", Property.of(p -> p.text(TextProperty.of(t -> t
                                                .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
                                        )))),
                                        Map.entry("outcome", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                        Map.entry("details", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard"))))),
                                        Map.entry("ip_address", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                        Map.entry("user_agent", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard"))))),
                                        Map.entry("workspace_id", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                        Map.entry("created_at", Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis")))))
                                ))
                        ))
                        .build();

                openSearchClient.indices().create(request);
                log.info("Created OpenSearch index: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize audit_events OpenSearch index", e);
        }
    }

    public void indexEvent(AuditEvent event) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", event.getId());
            doc.put("organization_id", event.getOrganization().getId());
            doc.put("user_id", event.getUser() != null ? event.getUser().getId() : null);
            doc.put("actor_name", event.getActorName());
            doc.put("event_type", event.getEventType());
            doc.put("category", event.getCategory() != null ? event.getCategory().name() : null);
            doc.put("resource_type", event.getResourceType());
            doc.put("resource_id", event.getResourceId());
            doc.put("resource_name", event.getResourceName());
            doc.put("outcome", event.getOutcome());
            doc.put("details", event.getDetails());
            doc.put("ip_address", event.getIpAddress());
            doc.put("user_agent", event.getUserAgent());
            doc.put("workspace_id", event.getWorkspace() != null ? event.getWorkspace().getId() : null);
            doc.put("created_at", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);

            openSearchClient.index(IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(event.getId().toString())
                    .document(doc)
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to index audit event", e);
        }
    }

    public SearchResult search(Long organizationId, String query, String category, String eventType,
                               Long userId, String outcome, Long workspaceId,
                               Instant dateFrom, Instant dateTo, int page, int size) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            // Always filter by organization
            boolBuilder.filter(Query.of(q -> q.term(t -> t
                    .field("organization_id").value(FieldValue.of(organizationId)))));

            // Full-text search
            if (query != null && !query.isBlank()) {
                boolBuilder.must(Query.of(q -> q.multiMatch(m -> m
                        .query(query)
                        .fields("actor_name", "resource_name", "details", "event_type", "ip_address")
                )));
            }

            // Filters
            if (category != null && !category.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("category").value(FieldValue.of(category)))));
            }
            if (eventType != null && !eventType.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("event_type").value(FieldValue.of(eventType)))));
            }
            if (userId != null) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("user_id").value(FieldValue.of(userId)))));
            }
            if (outcome != null && !outcome.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("outcome").value(FieldValue.of(outcome)))));
            }
            if (workspaceId != null) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("workspace_id").value(FieldValue.of(workspaceId)))));
            }
            if (dateFrom != null || dateTo != null) {
                boolBuilder.filter(Query.of(q -> q.range(RangeQuery.of(r -> {
                    r.field("created_at");
                    if (dateFrom != null) r.gte(org.opensearch.client.json.JsonData.of(dateFrom.toString()));
                    if (dateTo != null) r.lte(org.opensearch.client.json.JsonData.of(dateTo.toString()));
                    return r;
                }))));
            }

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(Query.of(q -> q.bool(boolBuilder.build())))
                    .from(page * size)
                    .size(size)
                    .sort(sort -> sort.field(f -> f.field("created_at").order(SortOrder.Desc)))
            );

            SearchResponse<Map> response = openSearchClient.search(request, Map.class);
            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            List<Map<String, Object>> hits = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    hits.add(hit.source());
                }
            }

            return new SearchResult(hits, total, page, size);
        } catch (Exception e) {
            log.error("OpenSearch audit search failed", e);
            return new SearchResult(List.of(), 0, page, size);
        }
    }

    public void deleteEventsBefore(Instant cutoff) {
        try {
            openSearchClient.deleteByQuery(DeleteByQueryRequest.of(d -> d
                    .index(INDEX_NAME)
                    .query(Query.of(q -> q.range(RangeQuery.of(r -> r
                            .field("created_at")
                            .lt(org.opensearch.client.json.JsonData.of(cutoff.toString()))
                    ))))
            ));
        } catch (Exception e) {
            log.error("Failed to delete old events from OpenSearch", e);
        }
    }

    public record SearchResult(List<Map<String, Object>> hits, long total, int page, int size) {}
}
