package com.sebatmal.devflow.api.task.dto;

import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(
        @NotNull(message = "주차는 필수입니다.") Integer week
) {
}
