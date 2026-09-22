package com.recallr.backend.study.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallr.backend.ai.service.OllamaService;
import com.recallr.backend.study.dto.AnswerEvaluation;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.session.ActiveQuestion;
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


        String typeInstructions = switch (type) {

                case MULTIPLE_CHOICE -> """
                        Create a multiple-choice question.

                                IMPORTANT:
                                - Provide exactly 4 answer options.
                                - Exactly ONE option must fully answer the question.
                                - The other 3 options must be clearly incorrect according
                                to the study material.
                                - Never create a question where multiple options are
                                individually correct.
                                - Never split a multi-part correct answer across several options.
                                - If the answer contains several elements, put ALL required
                                elements together in the single correct option.
                                - All 4 options must be different.
                                - Do not use duplicate options.
                                - Avoid "all of the above" and "none of the above".
                                - correctOptionIndex must identify the ONE fully correct option.
                                - correctOptionIndex must be 0, 1, 2, or 3.
                                - correctAnswer must be null.
                                """;

                case TRUE_FALSE -> """
                        Transform ONE fact from the study material into a
                        declarative statement.

                        The statement itself must be either true or false.

                        IMPORTANT:
                        - Write an AFFIRMATION, not a question.
                        - Do not ask for a definition.
                        - Do not use a question mark.
                        - The student must be able to answer only True or False.
                        - correctOptionIndex must be null.
                        - correctAnswer MUST be either "True" or "False".
                        """;

                case SHORT_ANSWER -> """
                        Create an open-ended short-answer question.
                        options must be [].
                        correctOptionIndex must be null.
                        correctAnswer must contain a concise expected answer.
                        """;
        };
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

                Specific instructions:
                %s

                Return exactly this JSON structure:

                {
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
                - Detect the language used in the study material.
                - Write ALL user-facing text in that same language.
                - Never switch to another language.
                - If the study material is in French, everything must be written in French.
                """.formatted(
                material.getConcept(),
                material.getContent(),
                type,
                typeInstructions
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

                JsonNode json =
                        objectMapper.readTree(aiResponse);

                List<String> options =
                        objectMapper.convertValue(
                                json.get("options"),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                String.class
                                        )
                        );

                Integer correctOptionIndex =
                        json.get("correctOptionIndex").isNull()
                                ? null
                                : json.get("correctOptionIndex").asInt();

                String correctAnswer =
                        json.get("correctAnswer").isNull()
                                ? null
                                : json.get("correctAnswer").asText();

                String questionText = json.get("question").asText();
                if (type == QuestionType.TRUE_FALSE) {
                options = List.of("True", "False");
                correctOptionIndex = null;

                questionText =
                        normalizeTrueFalseStatement(
                                questionText
                        );
                }

                GeneratedQuestion question =
                        new GeneratedQuestion(
                                type,
                                questionText,
                                options,
                                correctOptionIndex,
                                correctAnswer,
                                json.get("explanation").asText()
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

                        if (question.question().contains("?")) {
                                throw new IllegalArgumentException(
                                "True/False must be a statement, not a question"
                                );
                        }

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


    public AnswerEvaluation evaluateShortAnswer(
                StudyMaterial material,
                ActiveQuestion question,
                String userAnswer
        ) {

        String prompt = """
                You are an educational evaluator.

                Evaluate the student's answer using ONLY the provided
                study material.

                Concept:
                %s

                Study material:
                %s

                Question:
                %s

                Expected answer:
                %s

                Student answer:
                %s

                Evaluate conceptual understanding, not exact wording.

                A student may use different words and still be correct
                if the important concepts are present.

                Return exactly this JSON structure:

                {
                "score": 0,
                "correct": false,
                "feedback": "short feedback for the student",
                "explanation": "explanation of the correct answer",
                "expectedAnswer": "a good complete answer",
                "correctConcepts": [],
                "missingConcepts": []
                }

                Rules:
                - Return ONLY valid JSON.
                - score must be between 0 and 100.
                - correct should normally be true when score is 70 or higher.
                - Do not require exact wording.
                - Give credit for correct concepts.
                - Identify important missing concepts.
                - Do not introduce knowledge outside the study material.
                - Keep feedback concise and educational.
                - Use only the provided study material.
                - Write the question, options, answer, and explanation in the
                same language as the study material.
                - Do not switch languages.
                """.formatted(
                material.getConcept(),
                material.getContent(),
                question.question(),
                question.correctAnswer(),
                userAnswer
        );

        String json =
                ollamaService.generateJson(prompt);

        try {
                return objectMapper.readValue(
                        json,
                        AnswerEvaluation.class
                );
        } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to parse AI answer evaluation",
                        e
                );
        }
        }

        private String normalizeTrueFalseStatement(String text) {

                if (text == null) {
                        return null;
                }

                String normalized = text.trim();

                normalized = normalized.replaceAll(
                        "(?i)\\s*(c['’]est|est-ce)\\s+(vrai\\s+ou\\s+faux|vrai)\\s*\\?\\s*$",
                        ""
                );

                normalized = normalized.replaceAll(
                        "(?i)\\s*(vrai\\s+ou\\s+faux)\\s*\\?\\s*$",
                        ""
                );

                normalized = normalized.replaceAll(
                        "(?i)\\s*(true\\s+or\\s+false)\\s*\\?\\s*$",
                        ""
                );

                return normalized.trim();
                }
    
}