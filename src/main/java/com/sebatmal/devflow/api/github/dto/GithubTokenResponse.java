package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        String scope,
        // 실패 시 GitHub이 200으로 내려주는 에러 (예: incorrect_client_credentials, bad_verification_code)
        String error,
        @JsonProperty("error_description") String errorDescription
) {
}
