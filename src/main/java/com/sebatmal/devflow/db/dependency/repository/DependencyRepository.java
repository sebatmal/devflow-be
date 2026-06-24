package com.sebatmal.devflow.db.dependency.repository;

import com.sebatmal.devflow.db.dependency.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DependencyRepository extends JpaRepository<Dependency, Long> {

    // 프로젝트 내 모든 의존성 (fromTask 의 project 기준)
    List<Dependency> findByFromTaskProjectId(Long projectId);

    boolean existsByFromTaskIdAndToTaskId(Long fromTaskId, Long toTaskId);

    Optional<Dependency> findByFromTaskIdAndToTaskId(Long fromTaskId, Long toTaskId);

    List<Dependency> findByToTaskId(Long toTaskId);

    List<Dependency> findByFromTaskId(Long fromTaskId);
}
