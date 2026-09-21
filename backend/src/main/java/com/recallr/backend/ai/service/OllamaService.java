package com.recallr.backend.ai.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaService {

    private final RestClient restClient;

    public OllamaService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String generate(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "qwen3:4b",
                "prompt", prompt,
                "stream", false
        );

        OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null) {
            throw new IllegalStateException("No response received from Ollama");
        }

        return response.response();
    }

    private record OllamaResponse(String response) {
    }
}
