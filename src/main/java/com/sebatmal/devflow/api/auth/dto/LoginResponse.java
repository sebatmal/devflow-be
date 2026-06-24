package com.sebatmal.devflow.api.auth.dto;

import com.sebatmal.devflow.api.user.dto.MeResponse;

/**
 * FE(CallbackPage)가 code를 보내면 토큰을 받아가는 응답.
 * FE가 data.token 으로 바로 읽으므로 {data} 래퍼 없이 평평하게 반환한다 (auth 전용 계약).
 */
public record LoginResponse(
        String token,
        MeResponse user
) {
    public static LoginResponse from(final LoginResult result) {
        return new LoginResponse(result.accessToken(), MeResponse.from(result.user()));
    }
}
