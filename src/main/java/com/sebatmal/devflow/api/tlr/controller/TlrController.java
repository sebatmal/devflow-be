package com.sebatmal.devflow.api.tlr.controller;

import com.sebatmal.devflow.api.tlr.dto.TlrAnalysisRequest;
import com.sebatmal.devflow.api.tlr.dto.TlrAnalysisResult;
import com.sebatmal.devflow.api.tlr.service.TlrService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/tlr")
public class TlrController {

    private final TlrService tlrService;

    @PostMapping("/analyze")
    public ResponseEntity<APISuccessResponse<TlrAnalysisResult>> analyze(
            @RequestBody @Valid final TlrAnalysisRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.OK, tlrService.analyze(request));
    }

    @GetMapping("/health")
    public ResponseEntity<APISuccessResponse<String>> health() {
        return APISuccessResponse.of(HttpStatus.OK, "TLR service is running");
    }
}
