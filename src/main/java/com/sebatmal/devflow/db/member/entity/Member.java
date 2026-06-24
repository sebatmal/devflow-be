package com.sebatmal.devflow.db.member.entity;

import com.sebatmal.devflow.db.BaseTime;
import com.sebatmal.devflow.db.organization.entity.Organization;
import com.sebatmal.devflow.db.user.entity.User;
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
        name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "github_login"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "github_login", nullable = false)
    private String githubLogin;

    @Column(name = "github_id")
    private Long githubId;

    @Column(name = "name")
    private String name;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "role")
    private String role;

    @Column(name = "color")
    private String color;

    // 그 멤버가 로그인하면 연결됨 (org 소속 사람 → 활성 사용자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Member(final Organization organization, final String githubLogin, final Long githubId,
                  final String name, final String avatarUrl, final String role, final String color) {
        this.organization = organization;
        this.githubLogin = githubLogin;
        this.githubId = githubId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.color = color;
    }

    public void update(final String name, final String avatarUrl) {
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public void linkUser(final User user) {
        this.user = user;
    }
}
