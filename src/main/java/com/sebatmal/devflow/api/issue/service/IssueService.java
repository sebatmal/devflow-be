package com.sebatmal.devflow.api.issue.service;

import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.dto.GithubIssueResponse;
import com.sebatmal.devflow.api.issue.dto.CreateIssueRequest;
import com.sebatmal.devflow.api.issue.dto.CreatedIssueResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueService {

    private final UserRepository userRepository;
    private final GithubApiClient githubApiClient;

    public CreatedIssueResponse createIssue(
            final Long userId,
            final String owner,
            final String repo,
            final CreateIssueRequest request
    ) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_USER));

        if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_NO_GITHUB_TOKEN);
        }

        final GithubIssueResponse issue = githubApiClient.createIssue(
                user.getGithubAccessToken(),
                owner,
                repo,
                request.title(),
                request.body(),
                request.assignees(),
                request.labels()
        );
        return CreatedIssueResponse.from(issue);
    }
}
