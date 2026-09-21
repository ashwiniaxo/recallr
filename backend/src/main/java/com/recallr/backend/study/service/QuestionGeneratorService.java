package com.recallr.backend.study.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallr.backend.ai.service.OllamaService;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class QuestionGeneratorService {

    private final OllamaService ollamaService;
    private final StudyMaterialRepository studyMaterialRepository;
    private final ObjectMapper objectMapper;

    public QuestionGeneratorService(
            OllamaService ollamaService,
            StudyMaterialRepository studyMaterialRepository
    ) {
        this.ollamaService = ollamaService;
        this.studyMaterialRepository = studyMaterialRepository;
        this.objectMapper = new ObjectMapper();
    }

    public GeneratedQuestion generateQuestion(
            Long materialId,
            QuestionType type
    ) {

        StudyMaterial material = studyMaterialRepository
                .findById(materialId)
                .orElseThrow(() ->
                        new RuntimeException("Study material not found")
                );

        String prompt = """
                You are a study question generator.

                Generate ONE question using only the study material below.

                Concept:
                %s

                Study material:
                %s

                Question type:
                %s

                Rules:
                - Test understanding, not simple memorization.
                - Do not include information that is not in the study material.
                - Make the question clear.
                - For MULTIPLE_CHOICE, provide exactly 4 options.
                - For SHORT_ANSWER, return an empty options array.
                - For TRUE_FALSE, options must be ["True", "False"].
                - Return ONLY valid JSON.
                - Do not use Markdown.
                - Do not use ```json.

                Return exactly this structure:

                {
                  "type": "%s",
                  "question": "...",
                  "options": [],
                  "correctAnswer": "...",
                  "explanation": "..."
                }
                """.formatted(
                material.getConcept(),
                material.getContent(),
                type,
                type
        );

        String aiResponse = ollamaService.generate(prompt);

        try {
            return objectMapper.readValue(
                    aiResponse,
                    GeneratedQuestion.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse AI generated question: "
                            + aiResponse,
                    e
            );
        }
    }
}