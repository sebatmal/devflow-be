package com.sebatmal.devflow.api.github;

import com.sebatmal.devflow.api.github.dto.GithubTokenResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GithubOAuthClient {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";

    private final RestClient restClient = RestClient.create();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public GithubOAuthClient(
            @Value("${github.client-id:}") final String clientId,
            @Value("${github.client-secret:}") final String clientSecret,
            @Value("${github.redirect-uri:http://localhost:8080/api/auth/github/callback}") final String redirectUri,
            @Value("${github.scope:repo read:user read:org}") final String scope
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    public String buildAuthorizeUrl() {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .build()
                .toUriString();
    }

    public String exchangeCodeForToken(final String code) {
        // FE가 authorize 단계에서 redirect_uri 를 보내지 않으므로(OAuth App 기본 콜백 사용),
        // 토큰 교환에서도 redirect_uri 를 보내지 않아야 redirect_uri_mismatch 가 안 난다.
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);

        final GithubTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new DevflowException(FailMessage.GITHUB_OAUTH_FAILED);
                })
                .body(GithubTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            log.warn("GitHub 토큰 교환 실패 — error={}, description={}",
                    response == null ? "(응답 없음)" : response.error(),
                    response == null ? "" : response.errorDescription());
            throw new DevflowException(FailMessage.GITHUB_OAUTH_FAILED);
        }
        return response.accessToken();
    }
}
