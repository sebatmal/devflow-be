package com.sebatmal.devflow.api.tlr.dto;

import java.util.List;

public record TlrIssue(
        int issueNumber,
        String title,
        String body,
        List<String> labels,
        String state
) {
}
