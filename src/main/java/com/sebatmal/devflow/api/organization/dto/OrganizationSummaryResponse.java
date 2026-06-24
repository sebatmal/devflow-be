package com.sebatmal.devflow.api.organization.dto;

import com.sebatmal.devflow.api.github.dto.GithubOrgResponse;

/** 로그인 유저가 속한 org (선택 화면용). */
public record OrganizationSummaryResponse(
        String githubLogin,
        String avatarUrl,
        String description
) {
    public static OrganizationSummaryResponse from(final GithubOrgResponse org) {
        return new OrganizationSummaryResponse(org.login(), org.avatarUrl(), org.description());
    }
}
