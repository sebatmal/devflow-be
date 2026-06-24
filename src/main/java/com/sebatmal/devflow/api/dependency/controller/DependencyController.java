package com.sebatmal.devflow.api.dependency.controller;

import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.api.dependency.dto.CreateDependencyRequest;
import com.sebatmal.devflow.api.dependency.dto.DependencyResponse;
import com.sebatmal.devflow.api.dependency.service.DependencyService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/dependencies")
public class DependencyController {

    private final DependencyService dependencyService;

    // 의존성 생성: 위상정렬 순환 검증 후 저장 + GitHub blocked_by 반영
    @PostMapping
    public ResponseEntity<APISuccessResponse<DependencyResponse>> create(
            @Authentication final AuthCredential authCredential,
            @RequestBody @Valid final CreateDependencyRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.CREATED,
                dependencyService.create(authCredential.userId(), request.fromTaskId(), request.toTaskId()));
    }

    @DeleteMapping
    public ResponseEntity<APISuccessResponse<Void>> delete(
            @RequestParam("from") final Long fromTaskId,
            @RequestParam("to") final Long toTaskId
    ) {
        dependencyService.delete(fromTaskId, toTaskId);
        return APISuccessResponse.of(HttpStatus.OK, null);
    }
}
