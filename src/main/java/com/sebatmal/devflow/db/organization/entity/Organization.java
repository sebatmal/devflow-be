package com.sebatmal.devflow.db.organization.entity;

import com.sebatmal.devflow.db.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "organization")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_login", nullable = false, unique = true)
    private String githubLogin;

    @Column(name = "name")
    private String name;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Builder
    public Organization(final String githubLogin, final String name, final String avatarUrl) {
        this.githubLogin = githubLogin;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public void update(final String name, final String avatarUrl) {
        this.name = name;
        this.avatarUrl = avatarUrl;
    }
}
