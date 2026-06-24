package com.sebatmal.devflow.api.pullrequest.service;

import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.dto.GithubPullRequestData;
import com.sebatmal.devflow.api.pullrequest.dto.PullRequestResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.member.entity.Member;
import com.sebatmal.devflow.db.member.repository.MemberRepository;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.project.repository.ProjectRepository;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import com.sebatmal.devflow.enums.pr.PrReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PullRequestService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final GithubApiClient githubApiClient;

    /**
     * GitHub에서 PR 상태를 실시간(GraphQL 1쿼리)으로 가져와 보드용으로 가공한다.
     * 토큰/연동이 없으면 빈 목록(보드를 막지 않음).
     */
    public List<PullRequestResponse> getPullRequests(final Long userId, final Long projectId) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_PROJECT));

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_USER));

        final String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            return List.of();
        }

        final List<GithubPullRequestData> prs = githubApiClient.getPullRequests(
                token, project.getGithubOwner(), project.getGithubRepo());

        // github_login(소문자) -> memberId
        final Map<String, Long> loginToMemberId = new HashMap<>();
        for (final Member member : memberRepository.findByOrganizationId(project.getOrganization().getId())) {
            if (member.getGithubLogin() != null) {
                loginToMemberId.put(member.getGithubLogin().toLowerCase(), member.getId());
            }
        }

        return prs.stream()
                .filter(pr -> !"CLOSED".equalsIgnoreCase(pr.state())) // 머지 안 하고 닫힌 PR은 제외(노이즈)
                .map(pr -> toResponse(pr, loginToMemberId))
                .toList();
    }

    private PullRequestResponse toResponse(final GithubPullRequestData pr, final Map<String, Long> loginToMemberId) {
        final Long authorMemberId = memberId(pr.authorLogin(), loginToMemberId);
        final Long reviewerMemberId = pr.reviewerLogins().isEmpty()
                ? null : memberId(pr.reviewerLogins().get(0), loginToMemberId);

        return new PullRequestResponse(
                (long) pr.number(),
                pr.number(),
                pr.title(),
                authorMemberId,
                reviewerMemberId,
                resolveReview(pr).name(),
                ageDays(pr.createdAt()),
                pr.approvals(),
                pr.reviewerLogins().size(),
                pr.comments(),
                pr.url(),
                null
        );
    }

    private PrReview resolveReview(final GithubPullRequestData pr) {
        if ("MERGED".equalsIgnoreCase(pr.state())) {
            return PrReview.MERGED;
        }
        if ("CHANGES_REQUESTED".equalsIgnoreCase(pr.reviewDecision())) {
            return PrReview.CHANGES;
        }
        if ("APPROVED".equalsIgnoreCase(pr.reviewDecision())) {
            return PrReview.APPROVED;
        }
        return PrReview.WAIT;
    }

    private Long memberId(final String login, final Map<String, Long> loginToMemberId) {
        if (login == null) {
            return null;
        }
        return loginToMemberId.get(login.toLowerCase());
    }

    private int ageDays(final String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return 0;
        }
        try {
            final LocalDate created = OffsetDateTime.parse(createdAt).toLocalDate();
            return (int) Math.max(0, ChronoUnit.DAYS.between(created, LocalDate.now()));
        } catch (final Exception e) {
            log.warn("PR createdAt 파싱 실패(무시): {}", createdAt);
            return 0;
        }
    }
}
