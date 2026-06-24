package com.sebatmal.devflow.api.project.dto;

import com.sebatmal.devflow.db.project.entity.Project;

public record ProjectResponse(
        Long id,
        String name,
        String githubOwner,
        String githubRepo,
        String sprintLabel,
        Integer dDay
) {
    public static ProjectResponse from(final Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getGithubOwner(),
                project.getGithubRepo(),
                project.getSprintLabel(),
                project.getDDay()
        );
    }
}
