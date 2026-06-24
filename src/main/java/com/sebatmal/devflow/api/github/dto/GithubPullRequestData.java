package com.sebatmal.devflow.api.github.dto;

import java.util.List;

/** GraphQL로 가져온 PR 1건 (가공 후). */
public record GithubPullRequestData(
        int number,
        String title,
        String url,
        String state,          // OPEN | CLOSED | MERGED
        boolean draft,
        String createdAt,      // ISO-8601
        String authorLogin,
        String reviewDecision, // APPROVED | CHANGES_REQUESTED | REVIEW_REQUIRED | null
        int comments,
        int approvals,
        List<String> reviewerLogins
) {
}
