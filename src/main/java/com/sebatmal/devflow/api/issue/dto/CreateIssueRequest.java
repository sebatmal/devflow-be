package com.sebatmal.devflow.api.issue.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateIssueRequest(
        @NotBlank(message = "이슈 제목은 필수입니다.") String title,
        String body,
        List<String> assignees,
        List<String> labels
) {
}
