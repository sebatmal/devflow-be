package com.sebatmal.devflow.api.project.dto;

import com.sebatmal.devflow.db.member.entity.Member;

public record MemberResponse(
        Long id,
        String githubLogin,
        String name,
        String avatarUrl,
        String role,
        String color,
        Long userId
) {
    public static MemberResponse from(final Member member) {
        return new MemberResponse(
                member.getId(),
                member.getGithubLogin(),
                member.getName(),
                member.getAvatarUrl(),
                member.getRole(),
                member.getColor(),
                member.getUser() == null ? null : member.getUser().getId()
        );
    }
}
