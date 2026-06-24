package com.sebatmal.devflow.api.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubIssueResponse(
        Long id,
        Integer number,
        String title,
        String body,
        String state,
        @JsonProperty("html_url") String htmlUrl,
        List<Label> labels
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name) {
    }
}
