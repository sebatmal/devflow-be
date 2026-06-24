package com.sebatmal.devflow.db.organization.repository;

import com.sebatmal.devflow.db.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByGithubLogin(String githubLogin);
}
