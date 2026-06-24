package com.sebatmal.devflow.api.auth.jwt;

import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import org.springframework.stereotype.Component;

@Component
public class BearerAuthExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    public String extractTokenValue(final String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_EMPTY_HEADER);
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_INVALID_TOKEN);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
