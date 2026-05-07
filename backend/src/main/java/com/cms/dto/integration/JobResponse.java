package com.cms.dto.integration;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private String jobId;
    private String status;
    private int totalItems;
    private String message;
}
