package com.sebatmal.devflow.api.issue.dto;

import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;

public record CreatedIssueResponse(
        Integer number,
        String title,
        String state,
        String htmlUrl
) {
    public static CreatedIssueResponse from(final GithubIssueResponse issue) {
        return new CreatedIssueResponse(issue.number(), issue.title(), issue.state(), issue.htmlUrl());
    }
}
