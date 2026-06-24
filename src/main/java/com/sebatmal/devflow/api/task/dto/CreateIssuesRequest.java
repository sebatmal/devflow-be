package com.sebatmal.devflow.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateIssuesRequest(
        @NotEmpty(message = "이슈는 1개 이상이어야 합니다.") List<Item> items
) {
    public record Item(
            @NotBlank(message = "이슈 제목은 필수입니다.") String title,
            Long assigneeMemberId,
            // 같은 요청 내 선행 이슈의 index (0-based)
            List<Integer> depItemIndexes
    ) {
    }
}
