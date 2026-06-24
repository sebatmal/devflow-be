package com.sebatmal.devflow.api.pullrequest.controller;

import com.sebatmal.devflow.api.pullrequest.dto.PullRequestResponse;
import com.sebatmal.devflow.api.pullrequest.service.PullRequestService;
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
public class PullRequestController {

    private final PullRequestService pullRequestService;

    @GetMapping("/{projectId}/prs")
    public ResponseEntity<APISuccessResponse<List<PullRequestResponse>>> getPullRequests(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, pullRequestService.getPullRequests(projectId));
    }
}
