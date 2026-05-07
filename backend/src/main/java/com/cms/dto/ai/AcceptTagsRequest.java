package com.cms.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptTagsRequest {
    @NotNull
    private List<String> acceptedTags;
    @NotNull
    private List<String> rejectedTags;
}
