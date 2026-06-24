package com.sebatmal.devflow.api.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.db.task.repository.TaskRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI(LLM) 연동 서비스.
 *  - suggestIssues: Python issue-decomposer.py를 ProcessBuilder로 호출하여 기능 → 이슈 분해
 *  - suggestDependencies: 활성(미머지) 이슈만 대상으로 의존성 후보 추천 (추후 LLM)
 */
@Slf4j
@Service
public class AiStubService {

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final String pythonPath;
    private final String scriptPath;
    private final String anthropicApiKey;
    private final String anthropicBaseUrl;
    private final int timeoutSeconds;

    public AiStubService(
            TaskRepository taskRepository,
            ObjectMapper objectMapper,
            @Value("${ai.python-path:python3}") String pythonPath,
            @Value("${ai.script-path:scripts/issue-decomposer.py}") String scriptPath,
            @Value("${ai.anthropic-api-key:}") String anthropicApiKey,
            @Value("${ai.anthropic-base-url:}") String anthropicBaseUrl,
            @Value("${ai.timeout-seconds:60}") int timeoutSeconds
    ) {
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
        this.pythonPath = pythonPath;
        this.scriptPath = scriptPath;
        this.anthropicApiKey = anthropicApiKey;
        this.anthropicBaseUrl = anthropicBaseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Transactional(readOnly = true)
    public SuggestIssuesResponse suggestIssues(final Long featureId) {
        final Task feature = taskRepository.findById(featureId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_TASK));

        final String featureText = feature.getTitle();
        final String jsonOutput = callPythonScript(featureText);
        return parseScriptOutput(jsonOutput);
    }

    private String callPythonScript(final String featureText) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, scriptPath, "--json", "--feature", featureText
            );
            pb.environment().put("ANTHROPIC_API_KEY", anthropicApiKey);
            if (anthropicBaseUrl != null && !anthropicBaseUrl.isBlank()) {
                pb.environment().put("ANTHROPIC_BASE_URL", anthropicBaseUrl);
            }
            pb.redirectErrorStream(false);

            final Process process = pb.start();

            final String stdout = new String(
                    process.getInputStream().readAllBytes(), StandardCharsets.UTF_8
            );
            final String stderr = new String(
                    process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8
            );

            final boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("[AI] 스크립트 타임아웃 ({}초 초과)", timeoutSeconds);
                throw new DevflowException(FailMessage.AI_SCRIPT_ERROR);
            }

            final int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("[AI] 스크립트 종료 코드={}, stderr={}", exitCode, stderr);
                throw new DevflowException(FailMessage.AI_SCRIPT_ERROR);
            }

            log.info("[AI] 스크립트 실행 완료, stdout 길이={}", stdout.length());
            return stdout.trim();
        } catch (IOException | InterruptedException e) {
            log.error("[AI] 스크립트 실행 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DevflowException(FailMessage.AI_SCRIPT_ERROR, e);
        }
    }

    private SuggestIssuesResponse parseScriptOutput(final String jsonOutput) {
        try {
            final JsonNode root = objectMapper.readTree(jsonOutput);
            final JsonNode issuesNode = root.get("issues");

            final List<SuggestIssuesResponse.SuggestedIssue> suggestedIssues = new ArrayList<>();
            double totalDays = 0;

            for (final JsonNode issueNode : issuesNode) {
                final String title = issueNode.get("title").asText();
                final double estimatedDays = issueNode.get("estimated_days").asDouble();
                final double confidence = issueNode.get("confidence").asDouble();

                final String importance;
                if (confidence >= 0.8) {
                    importance = "high";
                } else if (confidence >= 0.5) {
                    importance = "medium";
                } else {
                    importance = "low";
                }

                suggestedIssues.add(new SuggestIssuesResponse.SuggestedIssue(
                        title, importance, (int) Math.ceil(estimatedDays), List.of()
                ));
                totalDays += estimatedDays;
            }

            return new SuggestIssuesResponse(
                    "AI 분해 결과 (SPIDR 기반 수직 슬라이스)",
                    totalDays,
                    suggestedIssues
            );
        } catch (Exception e) {
            log.error("[AI] 스크립트 출력 파싱 실패: {}", jsonOutput, e);
            throw new DevflowException(FailMessage.AI_SCRIPT_ERROR, e);
        }
    }

    @Transactional(readOnly = true)
    public SuggestDependenciesResponse suggestDependencies(final Long projectId) {
        // 활성(미머지) 이슈만 AI에 보냄 — 완료 서브그래프는 동결
        final List<Task> activeTasks = taskRepository.findByProjectId(projectId).stream()
                .filter(Task::isActive)
                .toList();
        log.info("[스텁] suggestDependencies project={}, 활성 이슈 {}개 (LLM 연동 후 후보 반환 예정)", projectId, activeTasks.size());

        // TODO: LLM 호출로 교체. 지금은 후보 없음(스텁).
        return new SuggestDependenciesResponse(List.of());
    }
}
