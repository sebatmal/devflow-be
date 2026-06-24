package com.sebatmal.devflow.api.ai.service;

import com.sebatmal.devflow.api.ai.dto.SuggestDependenciesResponse;
import com.sebatmal.devflow.api.ai.dto.SuggestIssuesResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.db.task.repository.TaskRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI(LLM) 연동 전 스텁. 시그니처/계약만 고정하고, 본문은 추후 LLM 호출로 교체.
 *  - suggestIssues: 기능 → 이슈 분해 추천 (SPIDR·INVEST 기반 — 추후 LLM)
 *  - suggestDependencies: 활성(미머지) 이슈만 대상으로 의존성 후보 추천 (추후 LLM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiStubService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public SuggestIssuesResponse suggestIssues(final Long featureId) {
        final Task feature = taskRepository.findById(featureId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_TASK));

        // TODO: LLM 호출로 교체. 지금은 수직 슬라이스 기본 분해를 반환(스텁).
        return new SuggestIssuesResponse(
                "[스텁] '" + feature.getTitle() + "' — 핵심 동작을 먼저, 검증·테스트는 그 이후로 (SPIDR·INVEST). 실제 추천은 LLM 연동 후 제공됩니다.",
                4,
                List.of(
                        new SuggestIssuesResponse.SuggestedIssue("핵심 동작 구현 (happy path)", "high", 2, List.of()),
                        new SuggestIssuesResponse.SuggestedIssue("입력 검증 및 예외 처리", "medium", 1, List.of(0)),
                        new SuggestIssuesResponse.SuggestedIssue("단위 테스트 작성", "low", 1, List.of(0))
                )
        );
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
