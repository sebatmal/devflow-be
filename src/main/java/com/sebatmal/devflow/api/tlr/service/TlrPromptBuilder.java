package com.sebatmal.devflow.api.tlr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.api.tlr.dto.TlrIssue;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TlrPromptBuilder {

    private final ObjectMapper objectMapper;

    public String buildSystemPrompt() {
        return """
                너는 소프트웨어 공학 기반 TLR(Traceability Link Recovery) 도우미다.
                GitHub 이슈 목록을 분석하여 이슈 간 의존성 trace link를 복구하라.

                [의존성 판정 기준 - 아래 7가지 중 하나에 해당해야만 의존성으로 인정]
                - API_CONTRACT: toIssue가 fromIssue의 API endpoint, request/response 구조를 사용한다.
                - DATA: toIssue가 fromIssue의 DB table, column, entity를 사용한다.
                - AUTH: toIssue가 fromIssue의 로그인, 토큰, 인증 로직을 전제한다.
                - WORKFLOW: 사용자 흐름상 fromIssue가 끝나야 toIssue를 검증할 수 있다.
                - COMMON_MODULE: toIssue가 fromIssue의 공통 모듈, 유틸을 사용한다.
                - INFRA: toIssue가 fromIssue의 인프라 설정(CI/CD, 환경변수, 서버 구성 등)을 전제한다.
                - TEST: toIssue의 테스트가 fromIssue의 구현을 전제한다.

                [제외 기준 - 아래에 해당하면 dependencies에 넣지 말고 excludedRelations에 넣어라]
                - 같은 label, assignee, milestone이라는 이유만으로 의존성을 추천하지 마라.
                - mock data로 병렬 개발이 가능한 경우는 의존성이 아니다.
                - confidence 0.75 미만인 관계는 제외한다.
                - fromIssueNumber == toIssueNumber인 자기 자신 의존은 제외한다.
                - 불확실한 추측 관계는 제외한다.

                [confidence 기준]
                - 0.9 이상: 이슈 본문에 API 경로, DB 테이블명, 인증 토큰 등 명확한 근거가 있다.
                - 0.75 이상: 제목과 본문을 함께 보면 강한 의존성이 있다.
                - 0.75 미만: dependencies에 절대 넣지 마라.

                [출력 형식 - 반드시 JSON만 출력하라. 마크다운, 설명 문장 출력 금지]
                {
                  "dependencies": [
                    {
                      "fromIssueNumber": 선행_이슈_번호,
                      "toIssueNumber": 후행_이슈_번호,
                      "type": "API_CONTRACT|DATA|AUTH|WORKFLOW|COMMON_MODULE|INFRA|TEST 중 하나",
                      "strength": "HARD|SOFT 중 하나",
                      "pdmType": "FS|SS|FF|SF 중 하나",
                      "reason": "왜 선행되어야 하는지 한 문장",
                      "evidence": {
                        "fromIssueEvidence": "선행 이슈 본문에서 가져온 근거 문구",
                        "toIssueEvidence": "후행 이슈 본문에서 가져온 근거 문구"
                      },
                      "confidence": 0.0~1.0
                    }
                  ],
                  "excludedRelations": [
                    {
                      "fromIssueNumber": 번호,
                      "toIssueNumber": 번호,
                      "reason": "제외 이유"
                    }
                  ]
                }
                """;
    }

    public String buildUserPrompt(final List<TlrIssue> issues) {
        try {
            final String issuesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(issues);
            return "다음 GitHub 이슈들의 의존 관계를 분석하라:\n" + issuesJson;
        } catch (final JsonProcessingException e) {
            throw new DevflowException(FailMessage.INTERNAL_SERVER_ERROR);
        }
    }
}
