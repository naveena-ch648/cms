package com.cms.service;

import com.cms.config.QdrantConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.*;
import io.qdrant.client.grpc.Points.ScoredPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.ConditionFactory.*;
import static io.qdrant.client.PointIdFactory.id;

@Service
public class VectorSearchService {

    private final QdrantClient qdrantClient;
    private final QdrantConfig qdrantConfig;

    public VectorSearchService(QdrantClient qdrantClient, QdrantConfig qdrantConfig) {
        this.qdrantClient = qdrantClient;
        this.qdrantConfig = qdrantConfig;
    }

    /**
     * Search for similar document chunks, filtered by accessible document IDs.
     */
    public List<ScoredPoint> search(List<Float> queryVector, List<String> accessibleDocumentIds,
                                     Long workspaceId, int limit) throws ExecutionException, InterruptedException {
        Filter.Builder filterBuilder = Filter.newBuilder();

        // Filter by workspace
        filterBuilder.addMust(matchKeyword("workspace_id", workspaceId.toString()));

        // Filter by accessible documents (RBAC) - use should conditions with match for each document
        if (accessibleDocumentIds != null && !accessibleDocumentIds.isEmpty()) {
            Filter.Builder docFilter = Filter.newBuilder();
            for (String docId : accessibleDocumentIds) {
                docFilter.addShould(matchKeyword("document_id", docId));
            }
            filterBuilder.addMust(Condition.newBuilder().setFilter(docFilter.build()).build());
        }

        List<ScoredPoint> results = qdrantClient.searchAsync(
                SearchPoints.newBuilder()
                        .setCollectionName(qdrantConfig.getCollectionName())
                        .addAllVector(queryVector)
                        .setFilter(filterBuilder.build())
                        .setLimit(limit)
                        .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                        .build()
        ).get();

        return results;
    }

    /**
     * Delete all vectors for a specific document (for re-indexing).
     */
    public void deleteByDocumentId(String documentId) throws ExecutionException, InterruptedException {
        qdrantClient.deleteAsync(
                qdrantConfig.getCollectionName(),
                Filter.newBuilder()
                        .addMust(matchKeyword("document_id", documentId))
                        .build()
        ).get();
    }

    /**
     * Extract payload field as string from a scored point.
     */
    public static String getPayloadString(ScoredPoint point, String field) {
        if (point.getPayloadMap().containsKey(field)) {
            return point.getPayloadMap().get(field).getStringValue();
        }
        return null;
    }

    /**
     * Extract payload field as integer from a scored point.
     */
    public static int getPayloadInt(ScoredPoint point, String field) {
        if (point.getPayloadMap().containsKey(field)) {
            return (int) point.getPayloadMap().get(field).getIntegerValue();
        }
        return 0;
    }
}
