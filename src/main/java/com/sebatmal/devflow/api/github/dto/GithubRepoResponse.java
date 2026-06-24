package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepoResponse(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description
) {
}
