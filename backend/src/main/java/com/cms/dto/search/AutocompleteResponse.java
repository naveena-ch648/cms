package com.cms.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteResponse {

    private List<SearchResultDto> files;
    private List<String> recentSearches;
}
