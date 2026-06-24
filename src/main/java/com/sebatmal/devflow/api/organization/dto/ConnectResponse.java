package com.sebatmal.devflow.api.organization.dto;

import com.sebatmal.devflow.api.project.dto.MemberResponse;
import com.sebatmal.devflow.api.project.dto.ProjectResponse;

import java.util.List;

public record ConnectResponse(
        Long organizationId,
        String githubLogin,
        String name,
        List<ProjectResponse> projects,
        List<MemberResponse> members
) {
}
