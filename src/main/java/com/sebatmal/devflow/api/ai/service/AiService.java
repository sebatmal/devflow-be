package com.sebatmal.devflow.api.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse.SuggestedDependency;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse.SuggestedIssue;
import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.project.repository.ProjectRepository;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.db.task.repository.TaskRepository;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import com.sebatmal.devflow.enums.task.Lane;
import com.sebatmal.devflow.enums.task.TaskStatus;
import com.sebatmal.devflow.enums.task.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GithubApiClient githubApiClient;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SuggestIssuesResponse suggestIssues(final Long featureId) {
        final Task feature = taskRepository.findById(featureId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_TASK));

        final String systemPrompt = """
                당신은 소프트웨어 프로젝트 이슈 분해 전문가입니다.
                SPIDR·INVEST 원칙에 따라 기능을 독립적이고 수직 슬라이스된 이슈로 분해합니다.
                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "note": "분해 전략 한 줄 설명",
                  "total": 예상_총_작업일수(숫자),
                  "issues": [
                    {
                      "title": "이슈 제목",
                      "importance": "high|medium|low",
                      "days": 예상_작업일수(숫자),
                      "deps": [선행_이슈_인덱스_배열(0부터 시작)]
                    }
                  ]
                }
                """;

        final String userPrompt = "다음 기능을 이슈로 분해해주세요: " + feature.getTitle();
        final String raw = openAiClient.chat(systemPrompt, userPrompt, true);

        try {
            final JsonNode root = objectMapper.readTree(raw);
            final String note = root.path("note").asText();
            final double total = root.path("total").asDouble();
            final List<SuggestedIssue> issues = objectMapper.convertValue(
                    root.path("issues"), new TypeReference<>() {}
            );
            return new SuggestIssuesResponse(note, total, issues);
        } catch (final Exception e) {
            log.error("AI 응답 파싱 실패 (suggestIssues): {}", raw, e);
            throw new DevflowException(FailMessage.AI_API_ERROR);
        }
    }

    /**
     * 1. GitHub에서 lastSyncedIssueNumber 초과 + open 이슈만 fetch
     * 2. 새 이슈를 DB(Task)에 저장
     * 3. 전체 활성 이슈를 LLM에 전송해 의존성 추천
     * 4. lastSyncedIssueNumber 업데이트
     */
    @Transactional
    public SuggestDependenciesResponse suggestDependencies(final Long userId, final Long projectId) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_PROJECT));
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_USER));

        if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_NO_GITHUB_TOKEN);
        }

        // 1. GitHub에서 새 이슈 fetch
        final List<GithubIssueResponse> newGithubIssues = githubApiClient.getOpenIssuesSince(
                user.getGithubAccessToken(),
                project.getGithubOwner(),
                project.getGithubRepo(),
                project.getLastSyncedIssueNumber()
        );
        log.info("GitHub 새 이슈 {}개 fetch (projectId={}, since=#{})",
                newGithubIssues.size(), projectId, project.getLastSyncedIssueNumber());

        // 2. 새 이슈를 DB Task로 저장 (이미 존재하는 githubIssueNumber는 건너뜀)
        if (!newGithubIssues.isEmpty()) {
            final Set<Integer> existingIssueNumbers = taskRepository.findByProjectId(projectId).stream()
                    .map(Task::getGithubIssueNumber)
                    .filter(n -> n != null)
                    .collect(Collectors.toSet());

            for (final GithubIssueResponse gh : newGithubIssues) {
                if (existingIssueNumbers.contains(gh.number())) {
                    continue;
                }
                taskRepository.save(Task.builder()
                        .project(project)
                        .type(TaskType.ISSUE)
                        .title(gh.title())
                        .status(TaskStatus.OPEN)
                        .lane(Lane.BE)
                        .week(1)
                        .row(0)
                        .githubIssueNumber(gh.number())
                        .build());
            }

            final int maxIssueNumber = newGithubIssues.stream()
                    .mapToInt(GithubIssueResponse::number)
                    .max()
                    .orElse(project.getLastSyncedIssueNumber());
            project.updateLastSyncedIssueNumber(maxIssueNumber);
        }

        // 3. 전체 활성 이슈로 LLM 호출
        final List<Task> activeTasks = taskRepository.findByProjectId(projectId).stream()
                .filter(Task::isActive)
                .toList();

        if (activeTasks.isEmpty()) {
            return new SuggestDependenciesResponse(List.of());
        }

        final Set<Long> activeIds = activeTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        final String taskList = activeTasks.stream()
                .map(t -> "- id: %d, 제목: %s".formatted(t.getId(), t.getTitle()))
                .collect(Collectors.joining("\n"));

        final String systemPrompt = """
                당신은 소프트웨어 프로젝트 의존성 분석 전문가입니다.
                이슈 목록을 보고 "A가 완료되어야 B를 시작할 수 있다"는 선행 관계를 추천합니다.
                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "candidates": [
                    {
                      "fromTaskId": 선행_이슈_id(숫자),
                      "toTaskId": 후행_이슈_id(숫자),
                      "reason": "의존 이유 설명",
                      "confidence": 0.0~1.0_사이_신뢰도(숫자)
                    }
                  ]
                }
                존재하지 않는 id는 절대 사용하지 마세요. 순환 의존은 제안하지 마세요.
                """;

        final String userPrompt = "다음 이슈들의 의존 관계를 추천해주세요:\n" + taskList;
        final String raw = openAiClient.chat(systemPrompt, userPrompt, true);

        try {
            final JsonNode root = objectMapper.readTree(raw);
            final List<SuggestedDependency> candidates = new ArrayList<>();

            for (final JsonNode node : root.path("candidates")) {
                final long fromId = node.path("fromTaskId").asLong();
                final long toId = node.path("toTaskId").asLong();
                if (!activeIds.contains(fromId) || !activeIds.contains(toId) || fromId == toId) {
                    log.warn("AI가 잘못된 의존성 후보 반환: from={}, to={} — 제외됨", fromId, toId);
                    continue;
                }
                candidates.add(new SuggestedDependency(
                        fromId,
                        toId,
                        node.path("reason").asText(),
                        node.path("confidence").asDouble()
                ));
            }

            log.info("suggestDependencies 완료 (projectId={}, 활성이슈={}개, 후보={}개)",
                    projectId, activeTasks.size(), candidates.size());
            return new SuggestDependenciesResponse(candidates);
        } catch (final Exception e) {
            log.error("AI 응답 파싱 실패 (suggestDependencies): {}", raw, e);
            throw new DevflowException(FailMessage.AI_API_ERROR);
        }
    }
}
