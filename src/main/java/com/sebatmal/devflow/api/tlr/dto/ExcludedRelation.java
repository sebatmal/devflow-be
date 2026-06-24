package com.sebatmal.devflow.api.tlr.dto;

public record ExcludedRelation(
        int fromIssueNumber,
        int toIssueNumber,
        String reason
) {
}
