package com.recallr.backend.ai.service;

import java.util.Map;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final String model;

    public OllamaService(@Value("${recallr.ai.model:qwen3:4b}") String model) {
        this.model = model;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String generate(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
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

    public String generateJson(String prompt) {
        return generateJson(prompt, "json", false);
    }

    public String generateStructuredJson(String prompt, Map<String, Object> schema) {
        return generateJson(prompt, schema, true);
    }

    private String generateJson(String prompt, Object format, boolean structured) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", format,
                "think", false,
                "options", structured
                        ? Map.of("temperature", 0.7, "top_p", 0.8, "top_k", 20, "min_p", 0,
                                "num_ctx", 8192, "num_predict", 1800)
                        : Map.of("temperature", 0.3)
        );

        OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No response received from Ollama"
            );
        }

        if (response.response() == null ||
                response.response().isBlank()) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.response();
    }
}
