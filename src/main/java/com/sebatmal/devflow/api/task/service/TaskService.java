package com.sebatmal.devflow.api.task.service;

import com.sebatmal.devflow.api.dependency.dto.DependencyResponse;
import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;
import com.sebatmal.devflow.api.task.dto.CreateFeatureRequest;
import com.sebatmal.devflow.api.task.dto.CreateIssuesRequest;
import com.sebatmal.devflow.api.task.dto.CreateIssuesResponse;
import com.sebatmal.devflow.api.task.dto.TaskResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.dependency.entity.Dependency;
import com.sebatmal.devflow.db.dependency.repository.DependencyRepository;
import com.sebatmal.devflow.db.member.entity.Member;
import com.sebatmal.devflow.db.member.repository.MemberRepository;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.project.repository.ProjectRepository;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.db.task.repository.TaskRepository;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import com.sebatmal.devflow.enums.task.TaskStatus;
import com.sebatmal.devflow.enums.task.TaskType;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GithubApiClient githubApiClient;

    // ---- 조회 ----
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(final Long projectId) {
        final List<Task> tasks = taskRepository.findByProjectId(projectId);
        final Map<Long, List<Long>> depsByTask = new HashMap<>();
        for (final Dependency d : dependencyRepository.findByFromTaskProjectId(projectId)) {
            depsByTask.computeIfAbsent(d.getToTask().getId(), key -> new ArrayList<>())
                    .add(d.getFromTask().getId());
        }
        return tasks.stream()
                .map(t -> TaskResponse.from(t, depsByTask.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    // ---- 기능 추가 ----
    @Transactional
    public TaskResponse addFeature(final Long projectId, final CreateFeatureRequest request) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_PROJECT));

        final Task feature = taskRepository.save(Task.builder()
                .project(project)
                .type(TaskType.FEATURE)
                .title(request.title().trim())
                .status(TaskStatus.PLANNED)
                .lane(request.lane())
                .week(request.week())
                .row(nextRow(projectId, request.week()))
                .build());

        final List<Long> depIds = new ArrayList<>();
        if (request.depTaskIds() != null) {
            for (final Long depId : request.depTaskIds()) {
                final Task depTask = findTask(depId);
                saveDependencyIfAbsent(depTask, feature); // 새 기능은 sink → 사이클 불가
                depIds.add(depId);
            }
        }
        return TaskResponse.from(feature, depIds);
    }

    // ---- 주차 이동 (의존성 순서 검증) ----
    @Transactional
    public TaskResponse moveTask(final Long taskId, final int week) {
        final Task task = findTask(taskId);

        // 선행(이 작업이 기다리는 것)보다 앞설 수 없음
        for (final Dependency d : dependencyRepository.findByToTaskId(taskId)) {
            if (d.getFromTask().getWeek() > week) {
                throw new DevflowException(FailMessage.CONFLICT_INVALID_TASK_MOVE);
            }
        }
        // 후행(이 작업을 기다리는 것)보다 뒤로 갈 수 없음
        for (final Dependency d : dependencyRepository.findByFromTaskId(taskId)) {
            if (d.getToTask().getWeek() < week) {
                throw new DevflowException(FailMessage.CONFLICT_INVALID_TASK_MOVE);
            }
        }

        task.moveToWeek(week);
        final List<Long> deps = dependencyRepository.findByToTaskId(taskId)
                .stream().map(d -> d.getFromTask().getId()).toList();
        return TaskResponse.from(task, deps);
    }

    // ---- 이슈 분리 확정 (이슈 생성 + 의존성 + GitHub 이슈 생성) ----
    @Transactional
    public CreateIssuesResponse createIssues(final Long userId, final Long featureId, final CreateIssuesRequest request) {
        final Task feature = findTask(featureId);
        if (taskRepository.existsByParentId(featureId)) {
            throw new DevflowException(FailMessage.CONFLICT_DUPLICATE_FEATURE_ISSUES);
        }
        final Project project = feature.getProject();
        final int baseRow = nextRow(project.getId(), feature.getWeek());

        // 1) 이슈 Task 생성
        final List<CreateIssuesRequest.Item> items = request.items();
        final List<Task> issueTasks = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            final CreateIssuesRequest.Item item = items.get(i);
            final Member assignee = (item.assigneeMemberId() == null) ? null
                    : memberRepository.findById(item.assigneeMemberId()).orElse(null);
            issueTasks.add(taskRepository.save(Task.builder()
                    .project(project)
                    .type(TaskType.ISSUE)
                    .parent(feature)
                    .title(item.title().trim())
                    .status(TaskStatus.PLANNED)
                    .lane(feature.getLane())
                    .week(feature.getWeek())
                    .row(baseRow + i)
                    .assignee(assignee)
                    .build()));
        }
        feature.markSplit();

        // 2) 의존성: 내부(index) 또는 기능의 선행 상속
        final List<Task> featurePredecessors = dependencyRepository.findByToTaskId(featureId)
                .stream().map(Dependency::getFromTask).toList();
        final List<Dependency> created = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            final List<Integer> depIndexes = items.get(i).depItemIndexes();
            if (depIndexes != null && !depIndexes.isEmpty()) {
                for (final Integer idx : depIndexes) {
                    if (idx >= 0 && idx < issueTasks.size() && idx != i) {
                        addIfPresent(created, saveDependencyIfAbsent(issueTasks.get(idx), issueTasks.get(i)));
                    }
                }
            } else if (!featurePredecessors.isEmpty()) {
                for (final Task pred : featurePredecessors) {
                    addIfPresent(created, saveDependencyIfAbsent(pred, issueTasks.get(i)));
                }
            } else {
                addIfPresent(created, saveDependencyIfAbsent(feature, issueTasks.get(i)));
            }
        }

        // 3) GitHub 이슈 생성 + blocked_by (best-effort)
        syncToGithub(userId, project, feature, issueTasks, created);

        // 4) 응답
        final Map<Long, List<Long>> depsByTask = new HashMap<>();
        for (final Dependency d : created) {
            depsByTask.computeIfAbsent(d.getToTask().getId(), key -> new ArrayList<>())
                    .add(d.getFromTask().getId());
        }
        final List<TaskResponse> issueResponses = issueTasks.stream()
                .map(t -> TaskResponse.from(t, depsByTask.getOrDefault(t.getId(), List.of())))
                .toList();
        final List<DependencyResponse> depResponses = created.stream()
                .map(d -> DependencyResponse.of(d, d.getFromTask().getGithubIssueNumber() != null
                        && d.getToTask().getGithubIssueNumber() != null))
                .toList();
        return new CreateIssuesResponse(featureId, issueTasks.size(), issueResponses, depResponses);
    }

    private void syncToGithub(final Long userId, final Project project, final Task feature,
                              final List<Task> issueTasks, final List<Dependency> created) {
        final User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            return;
        }
        final String token = user.getGithubAccessToken();
        final String owner = project.getGithubOwner();
        final String repo = project.getGithubRepo();

        for (final Task issue : issueTasks) {
            try {
                final List<String> assignees = (issue.getAssignee() == null) ? null
                        : List.of(issue.getAssignee().getGithubLogin());
                final GithubIssueResponse gh = githubApiClient.createIssue(
                        token, owner, repo, issue.getTitle(),
                        "DevFlow: '" + feature.getTitle() + "' 기능에서 분리된 이슈", assignees,
                        List.of(issue.getLane().name()));
                if (gh != null && gh.number() != null) {
                    issue.linkGithubIssue(gh.number());
                }
            } catch (final Exception e) {
                log.warn("GitHub 이슈 생성 실패(무시): {} — {}", issue.getTitle(), e.getMessage());
            }
        }
        for (final Dependency d : created) {
            final Integer fromNum = d.getFromTask().getGithubIssueNumber();
            final Integer toNum = d.getToTask().getGithubIssueNumber();
            if (fromNum != null && toNum != null) {
                githubApiClient.addBlockedBy(token, owner, repo, toNum, fromNum);
            }
        }
    }

    private int nextRow(final Long projectId, final int week) {
        int max = 2;
        for (final Task t : taskRepository.findByProjectId(projectId)) {
            if (t.getWeek() == week) {
                max = Math.max(max, t.getRow());
            }
        }
        return max + 1;
    }

    private Dependency saveDependencyIfAbsent(final Task from, final Task to) {
        if (from.getId().equals(to.getId())
                || dependencyRepository.existsByFromTaskIdAndToTaskId(from.getId(), to.getId())) {
            return null;
        }
        return dependencyRepository.save(Dependency.builder().fromTask(from).toTask(to).build());
    }

    private void addIfPresent(final List<Dependency> list, final Dependency dependency) {
        if (dependency != null) {
            list.add(dependency);
        }
    }

    private Task findTask(final Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_TASK));
    }
}
