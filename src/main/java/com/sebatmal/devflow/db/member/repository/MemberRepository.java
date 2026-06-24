package com.sebatmal.devflow.db.member.repository;

import com.sebatmal.devflow.db.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByOrganizationId(Long organizationId);

    Optional<Member> findByOrganizationIdAndGithubLogin(Long organizationId, String githubLogin);

    Optional<Member> findByOrganizationIdAndGithubId(Long organizationId, Long githubId);
}
