package com.sebatmal.devflow.api.github;

import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;
import com.sebatmal.devflow.api.github.dto.GithubRepoResponse;
import com.sebatmal.devflow.api.github.dto.GithubUserResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GithubApiClient {

    private static final String ACCEPT_GITHUB_JSON = "application/vnd.github+json";

    private final RestClient restClient = RestClient.create();
    private final String apiBase;

    public GithubApiClient(@Value("${github.api-base}") final String apiBase) {
        this.apiBase = apiBase;
    }

    public GithubUserResponse getUser(final String accessToken) {
        return restClient.get()
                .uri(apiBase + "/user")
                .headers(headers -> applyAuth(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    final String errorBody = new String(res.getBody().readAllBytes());
                    log.error("GitHub API 에러 [GET /user] status={}, body={}", res.getStatusCode(), errorBody);
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubUserResponse.class);
    }

    public List<GithubUserResponse> getOrgMembers(final String accessToken, final String org) {
        final GithubUserResponse[] members = restClient.get()
                .uri(apiBase + "/orgs/{org}/members?per_page=100", org)
                .headers(headers -> applyAuth(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    final String errorBody = new String(res.getBody().readAllBytes());
                    log.error("GitHub API 에러 [GET /orgs/{}/members] status={}, body={}", org, res.getStatusCode(), errorBody);
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubUserResponse[].class);
        return members == null ? List.of() : Arrays.asList(members);
    }

    public List<GithubRepoResponse> getOrgRepos(final String accessToken, final String org) {
        final GithubRepoResponse[] repos = restClient.get()
                .uri(apiBase + "/orgs/{org}/repos?per_page=100&sort=updated", org)
                .headers(headers -> applyAuth(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    final String errorBody = new String(res.getBody().readAllBytes());
                    log.error("GitHub API 에러 [GET /orgs/{}/repos] status={}, body={}", org, res.getStatusCode(), errorBody);
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubRepoResponse[].class);
        return repos == null ? List.of() : Arrays.asList(repos);
    }

    public GithubIssueResponse createIssue(
            final String accessToken, final String owner, final String repo,
            final String title, final String body, final List<String> assignees, final List<String> labels
    ) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        if (body != null) {
            payload.put("body", body);
        }
        if (assignees != null && !assignees.isEmpty()) {
            payload.put("assignees", assignees);
        }
        if (labels != null && !labels.isEmpty()) {
            payload.put("labels", labels);
        }
        return restClient.post()
                .uri(apiBase + "/repos/{owner}/{repo}/issues", owner, repo)
                .headers(headers -> applyAuth(headers, accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    final String errorBody = new String(res.getBody().readAllBytes());
                    log.error("GitHub API 에러 [POST /repos/{}/{}/issues] status={}, body={}", owner, repo, res.getStatusCode(), errorBody);
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubIssueResponse.class);
    }

    /**
     * GitHub Issue Dependencies(blocked_by) 연동 — best-effort.
     * (2025.08 GA. 엔드포인트/바디 스펙이 변경될 수 있어, 실패해도 핵심 로직은 막지 않고 로그만 남긴다.)
     * 의미: toIssueNumber 가 fromIssueNumber 에 의해 blocked.
     */
    public boolean addBlockedBy(final String accessToken, final String owner, final String repo,
                                final Integer toIssueNumber, final Integer fromIssueNumber) {
        try {
            restClient.post()
                    .uri(apiBase + "/repos/{owner}/{repo}/issues/{number}/dependencies/blocked_by", owner, repo, toIssueNumber)
                    .headers(headers -> applyAuth(headers, accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("issue_number", fromIssueNumber))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (final Exception e) {
            log.warn("GitHub blocked_by 연동 실패(무시): repo={}/{}, {} blocked_by {} — {}",
                    owner, repo, toIssueNumber, fromIssueNumber, e.getMessage());
            return false;
        }
    }

    private void applyAuth(final org.springframework.http.HttpHeaders headers, final String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set("Accept", ACCEPT_GITHUB_JSON);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
    }

    public Optional<GithubUserResponse> getUserSafely(final String accessToken) {
        try {
            return Optional.ofNullable(getUser(accessToken));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}
