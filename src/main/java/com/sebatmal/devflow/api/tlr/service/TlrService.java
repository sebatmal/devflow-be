package com.sebatmal.devflow.api.tlr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.api.tlr.dto.DependencyCandidate;
import com.sebatmal.devflow.api.tlr.dto.ExcludedRelation;
import com.sebatmal.devflow.api.tlr.dto.TlrAnalysisRequest;
import com.sebatmal.devflow.api.tlr.dto.TlrAnalysisResult;
import com.sebatmal.devflow.api.tlr.dto.TlrIssue;
import com.sebatmal.devflow.api.tlr.service.DependencyValidator.ValidationResult;
import com.sebatmal.devflow.api.tlr.service.DagValidator.DagResult;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TlrService {

    private final LlmClient llmClient;
    private final TlrPromptBuilder promptBuilder;
    private final DependencyValidator dependencyValidator;
    private final DagValidator dagValidator;
    private final ObjectMapper objectMapper;

    public TlrAnalysisResult analyze(final TlrAnalysisRequest request) {
        final List<TlrIssue> issues = request.issues();
        final List<Integer> issueNumbers = issues.stream().map(TlrIssue::issueNumber).toList();

        // 1. 프롬프트 생성
        final String systemPrompt = promptBuilder.buildSystemPrompt();
        final String userPrompt = promptBuilder.buildUserPrompt(issues);

        // 2. LLM 호출
        final String rawResponse = llmClient.chat(systemPrompt, userPrompt);
        log.info("TLR LLM 응답 수신 (repository={}): {}", request.repository(), rawResponse);

        // 3. JSON 파싱
        final JsonNode root;
        final List<DependencyCandidate> rawCandidates;
        final List<ExcludedRelation> excludedRelations;
        try {
            root = objectMapper.readTree(rawResponse);
            rawCandidates = objectMapper.convertValue(root.path("dependencies"), new TypeReference<>() {});
            excludedRelations = objectMapper.convertValue(root.path("excludedRelations"), new TypeReference<>() {});
        } catch (final Exception e) {
            log.error("LLM 응답 파싱 실패: {}", rawResponse, e);
            throw new DevflowException(FailMessage.AI_API_ERROR);
        }

        // 4. 기본 검증
        final ValidationResult validation = dependencyValidator.validate(rawCandidates, issues);
        if (!validation.errors().isEmpty()) {
            log.warn("TLR 검증 실패 ({}건): {}", validation.errors().size(), validation.errors());
            return validationFailed(validation.errors(), excludedRelations, rawResponse);
        }

        // 5. DAG 순환 탐지 + 위상정렬
        final DagResult dagResult = dagValidator.analyze(issueNumbers, validation.valid());
        if (dagResult.hasCycle()) {
            log.warn("TLR 순환 의존 탐지: {}", dagResult.cycle());
            return cycleFailed(dagResult.cycle(), validation.valid(), excludedRelations, rawResponse);
        }

        // 6. 성공
        log.info("TLR 분석 완료 (repository={}, 의존성={}건, 개발순서={})",
                request.repository(), validation.valid().size(), dagResult.topologicalOrder());
        return success(validation.valid(), excludedRelations, dagResult.topologicalOrder(), rawResponse);
    }

    private TlrAnalysisResult validationFailed(
            final List<String> errors,
            final List<ExcludedRelation> excludedRelations,
            final String rawResponse
    ) {
        return new TlrAnalysisResult(
                false,
                "검증 실패: " + errors.get(0),
                List.of(),
                excludedRelations,
                errors,
                List.of(),
                List.of(),
                rawResponse
        );
    }

    private TlrAnalysisResult cycleFailed(
            final List<Integer> cycle,
            final List<DependencyCandidate> candidates,
            final List<ExcludedRelation> excludedRelations,
            final String rawResponse
    ) {
        return new TlrAnalysisResult(
                false,
                "순환 의존성이 탐지되었습니다: " + cycle,
                candidates,
                excludedRelations,
                List.of(),
                cycle,
                List.of(),
                rawResponse
        );
    }

    private TlrAnalysisResult success(
            final List<DependencyCandidate> candidates,
            final List<ExcludedRelation> excludedRelations,
            final List<Integer> developmentOrder,
            final String rawResponse
    ) {
        return new TlrAnalysisResult(
                true,
                "TLR 분석 완료. 검증 통과.",
                candidates,
                excludedRelations,
                List.of(),
                List.of(),
                developmentOrder,
                rawResponse
        );
    }
}
