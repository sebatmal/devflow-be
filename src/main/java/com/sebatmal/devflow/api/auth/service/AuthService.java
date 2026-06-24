package com.sebatmal.devflow.api.auth.service;

import com.sebatmal.devflow.api.auth.dto.LoginResult;
import com.sebatmal.devflow.api.auth.jwt.JwtUtil;
import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.GithubOAuthClient;
import com.sebatmal.devflow.api.github.dto.GithubUserResponse;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GithubOAuthClient githubOAuthClient;
    private final GithubApiClient githubApiClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public String authorizeUrl() {
        return githubOAuthClient.buildAuthorizeUrl();
    }

    @Transactional
    public LoginResult login(final String code) {
        final String githubAccessToken = githubOAuthClient.exchangeCodeForToken(code);
        final GithubUserResponse githubUser = githubApiClient.getUser(githubAccessToken);

        final User user = userRepository.findByGithubId(githubUser.id())
                .map(existing -> {
                    existing.updateOnLogin(githubUser.login(), githubUser.name(), githubUser.avatarUrl(), githubAccessToken);
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId(githubUser.id())
                        .login(githubUser.login())
                        .name(githubUser.name())
                        .avatarUrl(githubUser.avatarUrl())
                        .githubAccessToken(githubAccessToken)
                        .build()));

        final String accessToken = jwtUtil.createAccessToken(user.getId());
        return new LoginResult(accessToken, user);
    }
}
