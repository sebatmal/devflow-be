package com.sebatmal.devflow.api.user.controller;

import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.api.user.dto.MeResponse;
import com.sebatmal.devflow.api.user.service.UserService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<APISuccessResponse<MeResponse>> getMe(
            @Authentication final AuthCredential authCredential
    ) {
        return APISuccessResponse.of(HttpStatus.OK, userService.getMe(authCredential.userId()));
    }
}
