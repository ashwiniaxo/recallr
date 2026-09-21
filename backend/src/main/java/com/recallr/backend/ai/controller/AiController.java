package com.recallr.backend.ai.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.ai.dto.AiRequest;
import com.recallr.backend.ai.dto.AiResponse;
import com.recallr.backend.ai.service.OllamaService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final OllamaService ollamaService;

    public AiController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/chat")
    public AiResponse chat(@RequestBody AiRequest request) {
        String response = ollamaService.generate(request.prompt());

        return new AiResponse(response);
    }
}
