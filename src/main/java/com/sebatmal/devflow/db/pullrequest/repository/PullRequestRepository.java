package com.sebatmal.devflow.db.pullrequest.repository;

import com.sebatmal.devflow.db.pullrequest.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {

    List<PullRequest> findByProjectId(Long projectId);
}
