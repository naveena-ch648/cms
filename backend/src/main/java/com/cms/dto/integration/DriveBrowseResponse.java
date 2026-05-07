package com.cms.dto.integration;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveBrowseResponse {
    private List<DriveItemResponse> items;
    private String nextPageToken;
}
