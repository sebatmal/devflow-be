package com.sebatmal.devflow.api.auth.dto;

import com.sebatmal.devflow.db.user.entity.User;

public record LoginResult(
        String accessToken,
        User user
) {
}
