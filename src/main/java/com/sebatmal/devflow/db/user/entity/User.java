package com.sebatmal.devflow.db.user.entity;

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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "name")
    private String name;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    // 데모용: 평문 저장. 실서비스에선 암호화/Vault 권장.
    @Column(name = "github_access_token", length = 512)
    private String githubAccessToken;

    @Builder
    public User(final Long githubId, final String login, final String name,
                final String avatarUrl, final String githubAccessToken) {
        this.githubId = githubId;
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.githubAccessToken = githubAccessToken;
    }

    public void updateOnLogin(final String login, final String name,
                              final String avatarUrl, final String githubAccessToken) {
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.githubAccessToken = githubAccessToken;
    }
}
