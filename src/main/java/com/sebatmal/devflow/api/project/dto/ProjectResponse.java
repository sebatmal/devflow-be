package com.sebatmal.devflow.api.project.dto;

import com.sebatmal.devflow.db.project.entity.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ProjectResponse(
        Long id,
        String name,
        String githubOwner,
        String githubRepo,
        String sprintLabel,
        Integer dDay,
        String connectedAt,   // org를 우리 서비스에 처음 연결한 날짜 (스프린트 기준일)
        int currentWeek       // 0-based 현재 주차 (1주차=0)
) {
    private static final int MAX_WEEK_INDEX = 4; // 5주차(0~4)

    public static ProjectResponse from(final Project project) {
        // 스프린트 타임라인은 org 단위 — org를 처음 연결한 시점(createdAt) 기준
        final LocalDateTime connectedAt = project.getOrganization().getCreatedAt();
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getGithubOwner(),
                project.getGithubRepo(),
                project.getSprintLabel(),
                project.getDDay(),
                connectedAt == null ? null : connectedAt.toLocalDate().toString(),
                computeCurrentWeek(connectedAt)
        );
    }

    // 현재 주차(0-based) = 연결일로부터 경과 주(=경과일/7), 0~4로 clamp.
    private static int computeCurrentWeek(final LocalDateTime connectedAt) {
        if (connectedAt == null) {
            return 0;
        }
        final long weeks = ChronoUnit.DAYS.between(connectedAt.toLocalDate(), LocalDate.now()) / 7;
        return (int) Math.max(0, Math.min(MAX_WEEK_INDEX, weeks));
    }
}
