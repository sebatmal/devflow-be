package com.sebatmal.devflow.db.dependency.entity;

import com.sebatmal.devflow.db.BaseTime;
import com.sebatmal.devflow.db.task.entity.Task;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 의존성 엣지. fromTask 가 끝나야 toTask 를 시작할 수 있다.
 * 즉 toTask is "blocked by" fromTask.
 */
@Getter
@Entity
@Table(
        name = "dependency",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_task_id", "to_task_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dependency extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_task_id", nullable = false)
    private Task fromTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_task_id", nullable = false)
    private Task toTask;

    @Builder
    public Dependency(final Task fromTask, final Task toTask) {
        this.fromTask = fromTask;
        this.toTask = toTask;
    }
}
