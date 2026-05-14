package com.cms.dto.digest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDigestPreferenceRequest {

    @NotNull
    private Boolean digestEnabled;

    @Pattern(regexp = "DAILY|WEEKLY", message = "digestFrequency must be DAILY or WEEKLY")
    private String digestFrequency;

    private Boolean includeSharedFiles;
    private Boolean includePendingApprovals;
    private Boolean includeStorageUsage;
    private Boolean includeRecentActivity;
}
