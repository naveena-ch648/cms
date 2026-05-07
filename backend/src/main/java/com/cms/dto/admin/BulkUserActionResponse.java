package com.cms.dto.admin;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUserActionResponse {

    private int totalRequested;
    private int successful;
    private int failed;
    private List<UserActionResult> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserActionResult {
        private String userId;
        private String status;
        private String reason;
    }
}
