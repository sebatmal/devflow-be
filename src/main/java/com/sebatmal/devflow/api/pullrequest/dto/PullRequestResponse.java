package com.sebatmal.devflow.api.pullrequest.dto;

import com.sebatmal.devflow.db.pullrequest.entity.PullRequest;

public record PullRequestResponse(
        Long id,
        Integer number,
        String title,
        Long authorMemberId,
        Long reviewerMemberId,
        String review,
        int ageDays,
        int approvals,
        int reviewers,
        int comments,
        String url,
        Long linkedTaskId
) {
    public static PullRequestResponse from(final PullRequest pr) {
        return new PullRequestResponse(
                pr.getId(),
                pr.getNumber(),
                pr.getTitle(),
                pr.getAuthor() == null ? null : pr.getAuthor().getId(),
                pr.getReviewer() == null ? null : pr.getReviewer().getId(),
                pr.getReview().name(),
                pr.getAgeDays(),
                pr.getApprovals(),
                pr.getReviewers(),
                pr.getComments(),
                pr.getUrl(),
                pr.getLinkedTask() == null ? null : pr.getLinkedTask().getId()
        );
    }
}
