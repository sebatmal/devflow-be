package com.sebatmal.devflow.api.tlr.dto;

public record DependencyCandidate(
        int fromIssueNumber,
        int toIssueNumber,
        String type,
        String strength,
        String pdmType,
        String reason,
        Evidence evidence,
        double confidence
) {
    public record Evidence(
            String fromIssueEvidence,
            String toIssueEvidence
    ) {
    }
}
