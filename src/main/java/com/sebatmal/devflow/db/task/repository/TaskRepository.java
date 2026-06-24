package com.sebatmal.devflow.db.task.repository;

import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.enums.task.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdAndType(Long projectId, TaskType type);

    List<Task> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);
}
