package com.sebatmal.devflow.api.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;
import com.sebatmal.devflow.api.github.dto.GithubOrgResponse;
import com.sebatmal.devflow.api.github.dto.GithubPullRequestData;
import com.sebatmal.devflow.api.github.dto.GithubRepoResponse;
import com.sebatmal.devflow.api.github.dto.GithubUserResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class GithubApiClient {

    private static final String ACCEPT_GITHUB_JSON = "application/vnd.github+json";
    private static final String GRAPHQL_URL = "https://api.github.com/graphql";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiBase;

    public GithubApiClient(@Value("${github.api-base:https://api.github.com}") final String apiBase) {
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

    // 로그인 유저가 속한 org 목록 (org 선택 화면용). read:org scope 필요.
    public List<GithubOrgResponse> getUserOrgs(final String accessToken) {
        final GithubOrgResponse[] orgs = restClient.get()
                .uri(apiBase + "/user/orgs?per_page=100")
                .headers(headers -> applyAuth(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubOrgResponse[].class);
        return orgs == null ? List.of() : Arrays.asList(orgs);
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

    /**
     * lastSyncedIssueNumber 초과 + open 상태인 이슈만 가져온다.
     * GitHub API는 since(날짜) 필터만 지원하므로 번호 필터는 클라이언트에서 처리한다.
     */
    public List<GithubIssueResponse> getOpenIssuesSince(
            final String accessToken, final String owner, final String repo, final int sinceIssueNumber
    ) {
        final GithubIssueResponse[] issues = restClient.get()
                .uri(apiBase + "/repos/{owner}/{repo}/issues?state=open&per_page=100&sort=created&direction=asc",
                        owner, repo)
                .headers(headers -> applyAuth(headers, accessToken))
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new DevflowException(FailMessage.GITHUB_API_ERROR);
                })
                .body(GithubIssueResponse[].class);

        if (issues == null) {
            return List.of();
        }
        return Arrays.stream(issues)
                .filter(issue -> issue.number() != null && issue.number() > sinceIssueNumber)
                .toList();
    }

    /**
     * GraphQL 1쿼리로 PR 목록 + 리뷰결과·승인수·코멘트수·리뷰어를 한 번에 가져온다(비용 최소화).
     * best-effort: 실패하면 빈 목록(안 터지게).
     */
    public List<GithubPullRequestData> getPullRequests(final String accessToken, final String owner, final String repo) {
        final String query = """
                query($owner:String!,$repo:String!){
                  repository(owner:$owner,name:$repo){
                    pullRequests(first:30, orderBy:{field:UPDATED_AT,direction:DESC}){
                      nodes{
                        number title url state isDraft createdAt
                        author{login}
                        reviewDecision
                        comments{totalCount}
                        reviews(first:50){nodes{author{login} state}}
                        reviewRequests(first:20){nodes{requestedReviewer{... on User{login}}}}
                      }
                    }
                  }
                }""";
        try {
            final Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("owner", owner, "repo", repo)
            );
            final String raw = restClient.post()
                    .uri(GRAPHQL_URL)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set("Accept", "application/json");
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            final JsonNode root = objectMapper.readTree(raw);
            if (root.has("errors")) {
                log.warn("GitHub GraphQL PR 조회 에러: {}", root.get("errors"));
                return List.of();
            }
            final JsonNode nodes = root.path("data").path("repository").path("pullRequests").path("nodes");
            final List<GithubPullRequestData> result = new ArrayList<>();
            for (final JsonNode n : nodes) {
                // 사용자별 마지막 리뷰 상태로 승인 수 계산
                final Map<String, String> latestReview = new LinkedHashMap<>();
                for (final JsonNode rv : n.path("reviews").path("nodes")) {
                    final JsonNode author = rv.path("author");
                    if (!author.isMissingNode() && !author.isNull()) {
                        latestReview.put(author.path("login").asText(), rv.path("state").asText(""));
                    }
                }
                final int approvals = (int) latestReview.values().stream().filter("APPROVED"::equals).count();
                final Set<String> reviewers = new LinkedHashSet<>(latestReview.keySet());
                for (final JsonNode rr : n.path("reviewRequests").path("nodes")) {
                    final JsonNode reviewer = rr.path("requestedReviewer");
                    if (reviewer.has("login")) {
                        reviewers.add(reviewer.path("login").asText());
                    }
                }
                final JsonNode authorNode = n.path("author");
                final String authorLogin = (authorNode.isMissingNode() || authorNode.isNull())
                        ? null : authorNode.path("login").asText(null);
                final JsonNode decisionNode = n.path("reviewDecision");
                final String reviewDecision = decisionNode.isNull() ? null : decisionNode.asText(null);

                result.add(new GithubPullRequestData(
                        n.path("number").asInt(),
                        n.path("title").asText(""),
                        n.path("url").asText(""),
                        n.path("state").asText(""),
                        n.path("isDraft").asBoolean(false),
                        n.path("createdAt").asText(null),
                        authorLogin,
                        reviewDecision,
                        n.path("comments").path("totalCount").asInt(0),
                        approvals,
                        new ArrayList<>(reviewers)
                ));
            }
            return result;
        } catch (final Exception e) {
            log.warn("GitHub GraphQL PR 조회 실패(무시): {}/{} — {}", owner, repo, e.getMessage());
            return List.of();
        }
    }

    public Optional<GithubUserResponse> getUserSafely(final String accessToken) {
        try {
            return Optional.ofNullable(getUser(accessToken));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}
