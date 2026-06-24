package com.sebatmal.devflow.api.tlr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TlrAnalysisRequest(
        @NotBlank String repository,
        @NotEmpty List<TlrIssue> issues
) {
}
