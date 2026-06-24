package com.sebatmal.devflow.api.task.dto;

import com.sebatmal.devflow.api.dependency.dto.DependencyResponse;

import java.util.List;

public record CreateIssuesResponse(
        Long featureId,
        int created,
        List<TaskResponse> issues,
        List<DependencyResponse> addedDeps
) {
}
