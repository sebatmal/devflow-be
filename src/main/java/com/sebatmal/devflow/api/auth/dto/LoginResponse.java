package com.sebatmal.devflow.api.auth.dto;

import com.sebatmal.devflow.api.user.dto.MeResponse;

public record LoginResponse(
        String accessToken,
        MeResponse user
) {
    public static LoginResponse from(final LoginResult result) {
        return new LoginResponse(result.accessToken(), MeResponse.from(result.user()));
    }
}
