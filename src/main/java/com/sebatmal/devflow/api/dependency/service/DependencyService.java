package com.sebatmal.devflow.api.dependency.service;

import com.sebatmal.devflow.api.dependency.dto.DependencyResponse;
import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.common.graph.DependencyGraph;
import com.sebatmal.devflow.db.dependency.entity.Dependency;
import com.sebatmal.devflow.db.dependency.repository.DependencyRepository;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.db.task.repository.TaskRepository;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DependencyService {

    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;
    private final UserRepository userRepository;
    private final GithubApiClient githubApiClient;

    /**
     * 의존성 생성: from→to (= to is blocked_by from).
     * 1) 위상정렬 기반 순환 검사 → 사이클이면 거부
     * 2) 저장
     * 3) 두 작업 모두 GitHub 이슈로 연결돼 있으면 blocked_by 반영(best-effort)
     */
    @Transactional
    public DependencyResponse create(final Long userId, final Long fromTaskId, final Long toTaskId) {
        if (fromTaskId.equals(toTaskId)) {
            throw new DevflowException(FailMessage.BAD_REQUEST_SELF_DEPENDENCY);
        }
        final Task fromTask = findTask(fromTaskId);
        final Task toTask = findTask(toTaskId);

        if (dependencyRepository.existsByFromTaskIdAndToTaskId(fromTaskId, toTaskId)) {
            throw new DevflowException(FailMessage.CONFLICT_DUPLICATE_DEPENDENCY);
        }

        final Long projectId = fromTask.getProject().getId();
        final Map<Long, List<Long>> adjacency = buildAdjacency(projectId);
        if (DependencyGraph.wouldCreateCycle(adjacency, fromTaskId, toTaskId)) {
            throw new DevflowException(FailMessage.CONFLICT_DEPENDENCY_CYCLE);
        }

        final Dependency saved = dependencyRepository.save(
                Dependency.builder().fromTask(fromTask).toTask(toTask).build());

        final boolean githubLinked = syncBlockedByToGithub(userId, fromTask, toTask);
        return DependencyResponse.of(saved, githubLinked);
    }

    @Transactional
    public void delete(final Long fromTaskId, final Long toTaskId) {
        final Dependency dependency = dependencyRepository.findByFromTaskIdAndToTaskId(fromTaskId, toTaskId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_DEPENDENCY));
        dependencyRepository.delete(dependency);
    }

    private Map<Long, List<Long>> buildAdjacency(final Long projectId) {
        final Map<Long, List<Long>> adjacency = new HashMap<>();
        for (final Dependency dependency : dependencyRepository.findByFromTaskProjectId(projectId)) {
            adjacency.computeIfAbsent(dependency.getFromTask().getId(), key -> new ArrayList<>())
                    .add(dependency.getToTask().getId());
        }
        return adjacency;
    }

    private boolean syncBlockedByToGithub(final Long userId, final Task fromTask, final Task toTask) {
        if (fromTask.getGithubIssueNumber() == null || toTask.getGithubIssueNumber() == null) {
            return false;
        }
        final User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            return false;
        }
        final Project project = fromTask.getProject();
        return githubApiClient.addBlockedBy(
                user.getGithubAccessToken(),
                project.getGithubOwner(),
                project.getGithubRepo(),
                toTask.getGithubIssueNumber(),
                fromTask.getGithubIssueNumber()
        );
    }

    private Task findTask(final Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_TASK));
    }
}
