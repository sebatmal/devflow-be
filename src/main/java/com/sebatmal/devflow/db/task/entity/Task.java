package com.sebatmal.devflow.db.task.entity;

import com.sebatmal.devflow.db.BaseTime;
import com.sebatmal.devflow.db.member.entity.Member;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.enums.task.Lane;
import com.sebatmal.devflow.enums.task.TaskStatus;
import com.sebatmal.devflow.enums.task.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TaskType type;

    // 이슈가 속한 기능 (FEATURE → ISSUE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parent;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "lane", nullable = false)
    private Lane lane;

    @Column(name = "week", nullable = false)
    private int week;

    @Column(name = "row_index", nullable = false)
    private int row;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_member_id")
    private Member assignee;

    @Column(name = "github_issue_number")
    private Integer githubIssueNumber;

    @Column(name = "is_split", nullable = false)
    private boolean split;

    @Builder
    public Task(final Project project, final TaskType type, final Task parent, final String title,
                final TaskStatus status, final Lane lane, final int week, final int row,
                final Member assignee, final Integer githubIssueNumber) {
        this.project = project;
        this.type = type;
        this.parent = parent;
        this.title = title;
        this.status = status;
        this.lane = lane;
        this.week = week;
        this.row = row;
        this.assignee = assignee;
        this.githubIssueNumber = githubIssueNumber;
        this.split = false;
    }

    public void moveToWeek(final int week) {
        this.week = week;
    }

    public void markSplit() {
        this.split = true;
    }

    public void assignTo(final Member member) {
        this.assignee = member;
    }

    public void linkGithubIssue(final Integer issueNumber) {
        this.githubIssueNumber = issueNumber;
    }

    public boolean isActive() {
        return status.isActive();
    }
}
