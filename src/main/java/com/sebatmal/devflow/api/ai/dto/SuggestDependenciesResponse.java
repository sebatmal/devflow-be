package com.sebatmal.devflow.api.ai.dto;

import java.util.List;

public record SuggestDependenciesResponse(
        List<SuggestedDependency> candidates
) {
    public record SuggestedDependency(
            Long fromTaskId,
            Long toTaskId,
            String reason,
            double confidence
    ) {
    }
}
