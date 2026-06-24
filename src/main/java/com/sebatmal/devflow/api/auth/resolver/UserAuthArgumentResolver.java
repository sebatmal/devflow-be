package com.sebatmal.devflow.api.auth.resolver;

import com.sebatmal.devflow.api.auth.jwt.BearerAuthExtractor;
import com.sebatmal.devflow.api.auth.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class UserAuthArgumentResolver implements HandlerMethodArgumentResolver {

    private final BearerAuthExtractor bearerAuthExtractor;
    private final JwtUtil jwtUtil;

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Authentication.class)
                && parameter.getParameterType().equals(AuthCredential.class);
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory
    ) {
        final HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String token = bearerAuthExtractor.extractTokenValue(header);
        final Long userId = jwtUtil.getUserId(token);
        return new AuthCredential(userId);
    }
}
