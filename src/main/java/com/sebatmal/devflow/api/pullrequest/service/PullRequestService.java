package com.sebatmal.devflow.api.pullrequest.service;

import com.sebatmal.devflow.api.pullrequest.dto.PullRequestResponse;
import com.sebatmal.devflow.db.pullrequest.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;

    public List<PullRequestResponse> getPullRequests(final Long projectId) {
        return pullRequestRepository.findByProjectId(projectId)
                .stream().map(PullRequestResponse::from).toList();
    }
}
