package com.sebatmal.devflow.api.issue.controller;

import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.api.issue.dto.CreateIssueRequest;
import com.sebatmal.devflow.api.issue.dto.CreatedIssueResponse;
import com.sebatmal.devflow.api.issue.service.IssueService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/repos")
public class IssueController {

    private final IssueService issueService;

    // POST /api/repos/{owner}/{repo}/issues  → 실제 GitHub 이슈 생성
    @PostMapping("/{owner}/{repo}/issues")
    public ResponseEntity<APISuccessResponse<CreatedIssueResponse>> createIssue(
            @Authentication final AuthCredential authCredential,
            @PathVariable("owner") final String owner,
            @PathVariable("repo") final String repo,
            @RequestBody @Valid final CreateIssueRequest request
    ) {
        return APISuccessResponse.of(
                HttpStatus.CREATED,
                issueService.createIssue(authCredential.userId(), owner, repo, request)
        );
    }
}
