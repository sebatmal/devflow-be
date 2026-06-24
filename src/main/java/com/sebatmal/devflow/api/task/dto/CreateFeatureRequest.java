package com.sebatmal.devflow.api.task.dto;

import com.sebatmal.devflow.enums.task.Lane;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateFeatureRequest(
        @NotBlank(message = "기능명은 필수입니다.") String title,
        @NotNull(message = "주차는 필수입니다.") Integer week,
        @NotNull(message = "분류(lane)는 필수입니다.") Lane lane,
        List<Long> depTaskIds
) {
}
