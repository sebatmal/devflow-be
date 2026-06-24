package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubIssueResponse(
        Long id,
        Integer number,
        String title,
        String state,
        @JsonProperty("html_url") String htmlUrl
) {
}
