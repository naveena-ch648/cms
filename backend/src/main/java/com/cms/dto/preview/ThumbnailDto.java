package com.cms.dto.preview;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThumbnailDto {

    private String url;
    private int width;
    private int height;
    private Instant expiresAt;
}
