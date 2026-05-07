package com.cms.service;

import com.cms.entity.MetadataValue;
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

import java.util.HashMap;
import java.util.List;
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
                                Map.entry("indexedAt", Property.of(p -> p.date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis"))))),
                                Map.entry("metadata", Property.of(p -> p.object(ObjectProperty.of(o -> o.dynamic(DynamicMapping.True))))),
                                Map.entry("tags", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
                        ))
                ))
                .build();

        openSearchClient.indices().create(request);
    }

    public void updateFileMetadata(String fileUuid, List<MetadataValue> values) {
        try {
            Map<String, Object> metadataMap = new HashMap<>();
            for (MetadataValue value : values) {
                String fieldName = value.getField().getName().toLowerCase().replaceAll("[^a-z0-9_]", "_");
                Object val = switch (value.getField().getFieldType()) {
                    case TEXT, DROPDOWN -> value.getTextValue();
                    case NUMBER -> value.getNumberValue();
                    case DATE -> value.getDateValue() != null ? value.getDateValue().toString() : null;
                };
                metadataMap.put(fieldName, val);
            }

            Map<String, Object> doc = new HashMap<>();
            doc.put("metadata", metadataMap);

            openSearchClient.update(u -> u
                    .index(indexName)
                    .id(fileUuid)
                    .doc(doc),
                    Map.class
            );
        } catch (Exception e) {
            log.warn("Failed to update metadata in search index for file {}: {}", fileUuid, e.getMessage());
        }
    }

    public void updateFileTags(String fileUuid, List<String> tagNames) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("tags", tagNames);

            openSearchClient.update(u -> u
                    .index(indexName)
                    .id(fileUuid)
                    .doc(doc),
                    Map.class
            );
        } catch (Exception e) {
            log.warn("Failed to update tags in search index for file {}: {}", fileUuid, e.getMessage());
        }
    }
}
