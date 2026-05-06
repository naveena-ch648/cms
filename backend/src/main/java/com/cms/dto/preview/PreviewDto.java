package com.cms.dto.preview;

import com.cms.entity.Preview;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewDto {

    private String id;
    private String fileId;
    private String type;
    private String status;
    private String mimeType;
    private int pageCount;
    private List<PageDto> pages;
    private String directUrl;
    private Instant expiresAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageDto {
        private int page;
        private String url;
        private int width;
        private int height;
    }

    public static PreviewDto from(Preview preview) {
        return PreviewDto.builder()
                .id(preview.getUuid())
                .fileId(preview.getFile().getUuid())
                .type(preview.getType().name())
                .status(preview.getStatus().name())
                .mimeType(preview.getMimeType())
                .pageCount(preview.getPageCount() != null ? preview.getPageCount() : 0)
                .build();
    }
}
