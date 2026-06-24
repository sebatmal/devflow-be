package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubOrgResponse(
        String login,
        Long id,
        @JsonProperty("avatar_url") String avatarUrl,
        String description
) {
}
