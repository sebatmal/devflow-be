package com.sebatmal.devflow.api.tlr.service;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
}
