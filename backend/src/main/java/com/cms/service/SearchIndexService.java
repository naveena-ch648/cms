package com.cms.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.*;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private final OpenSearchClient openSearchClient;

    @Value("${opensearch.index-name}")
    private String indexName;

    @PostConstruct
    public void initializeIndex() {
        try {
            boolean exists = openSearchClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                createIndex();
                log.info("Created OpenSearch index: {}", indexName);
            } else {
                log.info("OpenSearch index already exists: {}", indexName);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize OpenSearch index '{}': {}. Search will be unavailable until OpenSearch is running.", indexName, e.getMessage());
        }
    }

    private void createIndex() throws Exception {
        CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(IndexSettings.of(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                        .maxResultWindow(10000)
                        .analysis(IndexSettingsAnalysis.of(a -> a
                                .analyzer("file_name_analyzer", an -> an
                                        .custom(c -> c
                                                .tokenizer("standard")
                                                .filter("lowercase", "asciifolding")
                                        )
                                )
                        ))
                ))
                .mappings(TypeMapping.of(m -> m
                        .properties(Map.ofEntries(
                                Map.entry("fileUuid", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("fileName", Property.of(p -> p.text(TextProperty.of(t -> t
                                        .analyzer("file_name_analyzer")
                                        .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
                                )))),
                                Map.entry("content", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("standard"))))),
                                Map.entry("fileType", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("mimeType", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("ownerUuid", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("ownerName", Property.of(p -> p.text(TextProperty.of(t -> t
                                        .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
                                )))),
                                Map.entry("workspaceUuid", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("folderPath", Property.of(p -> p.text(TextProperty.of(t -> t
                                        .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
                                )))),
                                Map.entry("folderUuid", Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))),
                                Map.entry("fileSize", Property.of(p -> p.long_(LongNumberProperty.of(l -> l)))),
                                Map.entry("createdAt", Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis"))))),
                                Map.entry("updatedAt", Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis"))))),
                                Map.entry("indexedAt", Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis")))))
                        ))
                ))
                .build();

        openSearchClient.indices().create(request);
    }
}
