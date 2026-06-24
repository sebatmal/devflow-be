package com.sebatmal.devflow.api.ai.controller;

import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse;
import com.sebatmal.devflow.api.ai.service.AiStubService;
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

    private final AiStubService aiStubService;

    // AI 호출 ① — 기능 분해 추천 (현재 스텁, 추후 LLM)
    @PostMapping("/features/{featureId}/suggest-issues")
    public ResponseEntity<APISuccessResponse<SuggestIssuesResponse>> suggestIssues(
            @PathVariable("featureId") final Long featureId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, aiStubService.suggestIssues(featureId));
    }

    // AI 호출 ② — 의존성 추천 (활성 이슈만 대상, 현재 스텁, 추후 LLM)
    @PostMapping("/projects/{projectId}/suggest-dependencies")
    public ResponseEntity<APISuccessResponse<SuggestDependenciesResponse>> suggestDependencies(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, aiStubService.suggestDependencies(projectId));
    }
}
