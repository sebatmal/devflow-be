package com.sebatmal.devflow.api.auth.controller;

import com.sebatmal.devflow.api.auth.dto.LoginResponse;
import com.sebatmal.devflow.api.auth.dto.LoginResult;
import com.sebatmal.devflow.api.auth.service.AuthService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/auth/github")
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-redirect:}")
    private String frontendRedirect;

    // 1) 로그인 시작: GitHub 인증 페이지로 리다이렉트 (FE 버튼은 이 URL로 보내기만 하면 됨)
    @GetMapping("/login")
    public void login(final HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.authorizeUrl());
    }

    // 2) 콜백: code → 토큰 교환 → 우리 JWT 발급
    //    frontend-redirect 설정 시 그 주소로 ?token= 리다이렉트, 아니면 JSON 반환(Postman 테스트용)
    @GetMapping("/callback")
    public ResponseEntity<APISuccessResponse<LoginResponse>> callback(
            @RequestParam("code") final String code,
            final HttpServletResponse response
    ) throws IOException {
        final LoginResult result = authService.login(code);

        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            final String separator = frontendRedirect.contains("?") ? "&" : "?";
            response.sendRedirect(frontendRedirect + separator + "token=" + result.accessToken());
            return null;
        }
        return APISuccessResponse.of(HttpStatus.OK, LoginResponse.from(result));
    }
}
