package com.sebatmal.devflow.api.task.dto;

import com.sebatmal.devflow.db.task.entity.Task;

import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String type,
        Long parentId,
        String status,
        String lane,
        int week,
        int row,
        Long assigneeMemberId,
        Integer githubIssueNumber,
        boolean isSplit,
        List<Long> deps
) {
    public static TaskResponse from(final Task task, final List<Long> deps) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getType().name(),
                task.getParent() == null ? null : task.getParent().getId(),
                task.getStatus().name(),
                task.getLane().name(),
                task.getWeek(),
                task.getRow(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getGithubIssueNumber(),
                task.isSplit(),
                deps
        );
    }
}
