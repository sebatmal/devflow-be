package com.sebatmal.devflow.api.organization.controller;

import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.api.organization.dto.ConnectResponse;
import com.sebatmal.devflow.api.organization.dto.OrganizationSummaryResponse;
import com.sebatmal.devflow.api.organization.service.OrganizationService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    // 로그인 유저가 속한 org 목록 (선택 화면용)
    @GetMapping
    public ResponseEntity<APISuccessResponse<List<OrganizationSummaryResponse>>> getMyOrganizations(
            @Authentication final AuthCredential authCredential
    ) {
        return APISuccessResponse.of(HttpStatus.OK, organizationService.getMyOrganizations(authCredential.userId()));
    }

    // 특정 org 연결: GitHub에서 멤버·레포 불러와 upsert
    @PostMapping("/{org}/connect")
    public ResponseEntity<APISuccessResponse<ConnectResponse>> connect(
            @Authentication final AuthCredential authCredential,
            @PathVariable("org") final String org
    ) {
        return APISuccessResponse.of(HttpStatus.OK, organizationService.connect(authCredential.userId(), org));
    }
}
