package com.sebatmal.devflow.enums.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FailMessage {

    // 400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),
    BAD_REQUEST_BODY_VALID(HttpStatus.BAD_REQUEST, 40001, "요청 본문이 올바르지 않습니다."),
    BAD_REQUEST_MISSING_PARAM(HttpStatus.BAD_REQUEST, 40002, "필수 파라미터가 없습니다."),
    BAD_REQUEST_SELF_DEPENDENCY(HttpStatus.BAD_REQUEST, 40006, "자기 자신은 선행이 될 수 없습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증이 필요합니다."),
    UNAUTHORIZED_EXPIRED(HttpStatus.UNAUTHORIZED, 40101, "토큰이 만료되었습니다."),
    UNAUTHORIZED_EMPTY_HEADER(HttpStatus.UNAUTHORIZED, 40102, "인증 정보가 없습니다."),
    UNAUTHORIZED_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 40103, "토큰 정보가 올바르지 않습니다."),
    UNAUTHORIZED_NO_GITHUB_TOKEN(HttpStatus.UNAUTHORIZED, 40104, "GitHub 토큰이 없습니다. 다시 로그인해 주세요."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "권한이 없습니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "리소스를 찾을 수 없습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 40402, "유저를 찾을 수 없습니다."),
    NOT_FOUND_PROJECT(HttpStatus.NOT_FOUND, 40403, "프로젝트를 찾을 수 없습니다."),
    NOT_FOUND_TASK(HttpStatus.NOT_FOUND, 40404, "작업을 찾을 수 없습니다."),
    NOT_FOUND_DEPENDENCY(HttpStatus.NOT_FOUND, 40405, "의존성을 찾을 수 없습니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, 40406, "멤버를 찾을 수 없습니다."),

    // 409
    CONFLICT(HttpStatus.CONFLICT, 40900, "요청이 현재 상태와 충돌합니다."),
    CONFLICT_DEPENDENCY_CYCLE(HttpStatus.CONFLICT, 40901, "순환 의존이 생겨 추가할 수 없습니다."),
    CONFLICT_DUPLICATE_DEPENDENCY(HttpStatus.CONFLICT, 40902, "이미 연결된 의존성입니다."),
    CONFLICT_DUPLICATE_FEATURE_ISSUES(HttpStatus.CONFLICT, 40903, "이미 이슈가 생성된 기능입니다."),
    CONFLICT_INVALID_TASK_MOVE(HttpStatus.CONFLICT, 40904, "의존성 순서 때문에 이 주차로 옮길 수 없습니다."),

    // 500 / 502 (외부 GitHub · AI)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버 내부 오류가 발생했습니다."),
    AI_SCRIPT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "AI 스크립트 실행에 실패했습니다."),
    GITHUB_OAUTH_FAILED(HttpStatus.BAD_GATEWAY, 50200, "GitHub 인증에 실패했습니다."),
    GITHUB_API_ERROR(HttpStatus.BAD_GATEWAY, 50201, "GitHub API 호출에 실패했습니다."),
    AI_API_ERROR(HttpStatus.BAD_GATEWAY, 50202, "AI API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
