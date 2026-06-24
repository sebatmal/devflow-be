package com.sebatmal.devflow.api.tlr.dto;

import java.util.List;

public record TlrAnalysisResult(
        boolean valid,
        String message,
        List<DependencyCandidate> validatedDependencies,
        List<ExcludedRelation> excludedRelations,
        List<String> validationErrors,
        List<Integer> detectedCycle,
        List<Integer> developmentOrder,
        String claudeRawResponse
) {
}
