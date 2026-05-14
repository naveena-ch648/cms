package com.cms.dto.digest;

import com.cms.entity.UserEmailPreference;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDigestPreferenceDto {

    private boolean digestEnabled;
    private String digestFrequency;
    private boolean includeSharedFiles;
    private boolean includePendingApprovals;
    private boolean includeStorageUsage;
    private boolean includeRecentActivity;

    public static EmailDigestPreferenceDto from(UserEmailPreference pref) {
        return EmailDigestPreferenceDto.builder()
                .digestEnabled(pref.isDigestEnabled())
                .digestFrequency(pref.getDigestFrequency().name())
                .includeSharedFiles(pref.isIncludeSharedFiles())
                .includePendingApprovals(pref.isIncludePendingApprovals())
                .includeStorageUsage(pref.isIncludeStorageUsage())
                .includeRecentActivity(pref.isIncludeRecentActivity())
                .build();
    }
}
