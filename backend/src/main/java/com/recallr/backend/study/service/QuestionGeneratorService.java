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
    private static final int MAX_ATTEMPTS = 3;

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

                Generate exactly ONE study question using ONLY the
                study material provided below.

                Concept:
                %s

                Study material:
                %s

                Question type:
                %s

                Return exactly this JSON structure:

                {
                "type": "%s",
                "question": "question text",
                "options": [],
                "correctOptionIndex": null,
                "correctAnswer": null,
                "explanation": "brief explanation"
                }

                Rules:
                - Return ONLY valid JSON.
                - Never omit a field.
                - Do not add additional fields.
                - Do not use Markdown.
                - Use only the provided study material.

                For MULTIPLE_CHOICE:
                - Provide exactly 4 options.
                - Exactly one option must be correct.
                - correctOptionIndex must be the zero-based index
                of the correct option: 0, 1, 2, or 3.
                - correctAnswer must be null.

                For TRUE_FALSE:
                - options must be ["True", "False"].
                - correctOptionIndex must be null.
                - correctAnswer must be exactly "True" or "False".

                For SHORT_ANSWER:
                - options must be [].
                - correctOptionIndex must be null.
                - correctAnswer must contain a concise expected answer.
                """.formatted(
                material.getConcept(),
                material.getContent(),
                type,
                type
        );

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {
                String aiResponse =
                        ollamaService.generateJson(prompt);

                System.out.println(
                        "Question generation attempt "
                                + attempt + "/" + MAX_ATTEMPTS
                );

                System.out.println(aiResponse);

                GeneratedQuestion question =
                        objectMapper.readValue(
                                aiResponse,
                                GeneratedQuestion.class
                        );

                validateQuestion(question, type);

                return question;

            } catch (Exception e) {

                lastException = e;

                System.out.println(
                        "Question generation failed on attempt "
                                + attempt
                );

                System.out.println(e.getMessage());
            }
        }

        throw new RuntimeException(
                "Failed to generate a valid question after "
                        + MAX_ATTEMPTS + " attempts",
                lastException
        );
    }
    private void validateQuestion(
                GeneratedQuestion question,
                QuestionType expectedType
        ) {

        if (question.type() == null) {
                throw new IllegalArgumentException(
                        "Question type is missing"
                );
        }

        if (question.type() != expectedType) {
                throw new IllegalArgumentException(
                        "Unexpected question type"
                );
        }

        if (question.question() == null ||
                question.question().isBlank()) {
                throw new IllegalArgumentException(
                        "Question text is missing"
                );
        }

        if (question.explanation() == null ||
                question.explanation().isBlank()) {
                throw new IllegalArgumentException(
                        "Explanation is missing"
                );
        }

        switch (expectedType) {

                case MULTIPLE_CHOICE -> {

                if (question.options() == null ||
                        question.options().size() != 4) {
                        throw new IllegalArgumentException(
                                "Multiple choice must contain exactly 4 options"
                        );
                }

                if (question.correctOptionIndex() == null) {
                        throw new IllegalArgumentException(
                                "Correct option index is missing"
                        );
                }

                if (question.correctOptionIndex() < 0 ||
                        question.correctOptionIndex() >= 4) {
                        throw new IllegalArgumentException(
                                "Correct option index must be between 0 and 3"
                        );
                }

                if (question.correctAnswer() != null) {
                        throw new IllegalArgumentException(
                                "Multiple choice correctAnswer must be null"
                        );
                }
                }

                case TRUE_FALSE -> {

                if (question.options() == null ||
                        question.options().size() != 2 ||
                        !question.options().contains("True") ||
                        !question.options().contains("False")) {

                        throw new IllegalArgumentException(
                                "True/False options must be True and False"
                        );
                }

                if (question.correctOptionIndex() != null) {
                        throw new IllegalArgumentException(
                                "True/False correctOptionIndex must be null"
                        );
                }

                if (question.correctAnswer() == null ||
                        (!question.correctAnswer().equals("True") &&
                        !question.correctAnswer().equals("False"))) {

                        throw new IllegalArgumentException(
                                "True/False answer must be True or False"
                        );
                }
                }

                case SHORT_ANSWER -> {

                if (question.options() == null ||
                        !question.options().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Short answer options must be empty"
                        );
                }

                if (question.correctOptionIndex() != null) {
                        throw new IllegalArgumentException(
                                "Short answer correctOptionIndex must be null"
                        );
                }

                if (question.correctAnswer() == null ||
                        question.correctAnswer().isBlank()) {
                        throw new IllegalArgumentException(
                                "Short answer correctAnswer is missing"
                        );
                }
                }
        }
    }
    
}