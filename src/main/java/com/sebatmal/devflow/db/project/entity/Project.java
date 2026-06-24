package com.sebatmal.devflow.db.project.entity;

import com.sebatmal.devflow.db.BaseTime;
import com.sebatmal.devflow.db.organization.entity.Organization;
import jakarta.persistence.Column;
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

@Getter
@Entity
@Table(
        name = "project",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "github_repo"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(name = "name")
    private String name;

    @Column(name = "sprint_label")
    private String sprintLabel;

    @Column(name = "d_day")
    private Integer dDay;

    @Builder
    public Project(final Organization organization, final String githubRepo, final String name,
                   final String sprintLabel, final Integer dDay) {
        this.organization = organization;
        this.githubRepo = githubRepo;
        this.name = name;
        this.sprintLabel = sprintLabel;
        this.dDay = dDay;
    }

    public String getGithubOwner() {
        return organization.getGithubLogin();
    }
}
