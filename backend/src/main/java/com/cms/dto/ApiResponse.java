package com.cms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ApiError error;
    private Meta meta;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, PagedMeta pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .pagination(pagination)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder().code(code).message(message).build())
                .meta(Meta.now())
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiError {
        private String code;
        private String message;
        @Builder.Default
        private java.util.List<String> details = new java.util.ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private Instant timestamp;
        private String requestId;
        private PagedMeta pagination;

        public static Meta now() {
            return Meta.builder()
                    .timestamp(Instant.now())
                    .requestId(UUID.randomUUID().toString())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagedMeta {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
