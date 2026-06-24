package com.sebatmal.devflow.db.user.repository;

import com.sebatmal.devflow.db.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(Long githubId);
}
