package com.sebatmal.devflow.api.user.dto;

import com.sebatmal.devflow.db.user.entity.User;

public record MeResponse(
        Long id,
        Long githubId,
        String login,
        String name,
        String avatarUrl
) {
    public static MeResponse from(final User user) {
        return new MeResponse(
                user.getId(),
                user.getGithubId(),
                user.getLogin(),
                user.getName(),
                user.getAvatarUrl()
        );
    }
}
