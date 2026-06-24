package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubUserResponse(
        Long id,
        String login,
        String name,
        @JsonProperty("avatar_url") String avatarUrl
) {
}
