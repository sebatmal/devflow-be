package com.sebatmal.devflow.api.dependency.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDependencyRequest(
        @NotNull(message = "fromTaskId는 필수입니다.") Long fromTaskId,
        @NotNull(message = "toTaskId는 필수입니다.") Long toTaskId
) {
}
