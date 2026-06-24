package com.sebatmal.devflow.api.project.controller;

import com.sebatmal.devflow.api.project.dto.MemberResponse;
import com.sebatmal.devflow.api.project.dto.ProjectResponse;
import com.sebatmal.devflow.api.project.service.ProjectService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{projectId}")
    public ResponseEntity<APISuccessResponse<ProjectResponse>> getProject(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, projectService.getProject(projectId));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<APISuccessResponse<List<MemberResponse>>> getMembers(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, projectService.getMembers(projectId));
    }
}
