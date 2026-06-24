package com.sebatmal.devflow.api.ai.controller;

import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse;
import com.sebatmal.devflow.api.ai.service.AiService;
import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}")
public class AiController {

    private final AiService aiService;

    @PostMapping("/features/{featureId}/suggest-issues")
    public ResponseEntity<APISuccessResponse<SuggestIssuesResponse>> suggestIssues(
            @PathVariable("featureId") final Long featureId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, aiService.suggestIssues(featureId));
    }

    @PostMapping("/projects/{projectId}/suggest-dependencies")
    public ResponseEntity<APISuccessResponse<SuggestDependenciesResponse>> suggestDependencies(
            @Authentication final AuthCredential authCredential,
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK,
                aiService.suggestDependencies(authCredential.userId(), projectId));
    }
}
