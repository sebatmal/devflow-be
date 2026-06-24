package com.sebatmal.devflow.db.pullrequest.entity;

import com.sebatmal.devflow.db.BaseTime;
import com.sebatmal.devflow.db.member.entity.Member;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.task.entity.Task;
import com.sebatmal.devflow.enums.pr.PrReview;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "pull_request",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "number"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PullRequest extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_member_id")
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_member_id")
    private Member reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "review", nullable = false)
    private PrReview review;

    @Column(name = "age_days")
    private int ageDays;

    @Column(name = "approvals")
    private int approvals;

    @Column(name = "reviewers")
    private int reviewers;

    @Column(name = "comments")
    private int comments;

    @Column(name = "url", columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_task_id")
    private Task linkedTask;

    @Builder
    public PullRequest(final Project project, final Integer number, final String title,
                       final Member author, final Member reviewer, final PrReview review,
                       final int ageDays, final int approvals, final int reviewers, final int comments,
                       final String url, final Task linkedTask) {
        this.project = project;
        this.number = number;
        this.title = title;
        this.author = author;
        this.reviewer = reviewer;
        this.review = review;
        this.ageDays = ageDays;
        this.approvals = approvals;
        this.reviewers = reviewers;
        this.comments = comments;
        this.url = url;
        this.linkedTask = linkedTask;
    }
}
