package com.sebatmal.devflow.api.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(
            @Value("${openai.api-key}") final String apiKey,
            @Value("${openai.model:gpt-4o-mini}") final String model,
            final ObjectMapper objectMapper
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않았습니다. AI 기능이 동작하지 않습니다.");
        }
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_CHAT_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * system + user 메시지를 보내고 응답 텍스트를 반환한다.
     * JSON 모드가 필요하면 systemPrompt에 "JSON으로만 응답" 지시를 포함하고 responseFormat=json_object를 넘긴다.
     */
    public String chat(final String systemPrompt, final String userPrompt, final boolean jsonMode) {
        final Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", jsonMode
                        ? Map.of("type", "json_object")
                        : Map.of("type", "text"),
                "temperature", 0.3
        );

        try {
            final String raw = restClient.post()
                    .body(body)
                    .retrieve()
                    .body(String.class);

            final JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (final Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage(), e);
            throw new DevflowException(FailMessage.AI_API_ERROR);
        }
    }
}
