package com.sebatmal.devflow.api.ai.dto;

import java.util.List;

public record SuggestIssuesResponse(
        String note,
        double total,
        List<SuggestedIssue> issues
) {
    public record SuggestedIssue(
            String title,
            String importance,
            int days,
            List<Integer> deps
    ) {
    }
}
