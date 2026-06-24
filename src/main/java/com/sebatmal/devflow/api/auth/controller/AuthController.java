package com.sebatmal.devflow.api.auth.controller;

import com.sebatmal.devflow.api.auth.dto.LoginResponse;
import com.sebatmal.devflow.api.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    // (선택) BE 주도 로그인 시작 — FE가 직접 authorize로 보내면 안 써도 됨
    @GetMapping("/login")
    public void login(final HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.authorizeUrl());
    }

    // FE CallbackPage가 GitHub에서 받은 code를 전달 → 토큰 교환 후 JSON({ token, user }) 반환.
    // FE가 data.token 으로 바로 읽으므로 {data} 래퍼 없이 평평하게 반환한다 (auth 전용 계약).
    @GetMapping("/callback")
    public ResponseEntity<LoginResponse> callback(@RequestParam("code") final String code) {
        return ResponseEntity.ok(LoginResponse.from(authService.login(code)));
    }
}
