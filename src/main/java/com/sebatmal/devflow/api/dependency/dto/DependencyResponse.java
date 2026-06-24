package com.sebatmal.devflow.api.dependency.dto;

import com.sebatmal.devflow.db.dependency.entity.Dependency;

public record DependencyResponse(
        Long id,
        Long fromTaskId,
        Long toTaskId,
        boolean githubLinked
) {
    public static DependencyResponse of(final Dependency dependency, final boolean githubLinked) {
        return new DependencyResponse(
                dependency.getId(),
                dependency.getFromTask().getId(),
                dependency.getToTask().getId(),
                githubLinked
        );
    }
}
