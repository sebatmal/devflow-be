package com.sebatmal.devflow.db.project.repository;

import com.sebatmal.devflow.db.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOrganizationId(Long organizationId);

    Optional<Project> findByOrganizationIdAndGithubRepo(Long organizationId, String githubRepo);
}
