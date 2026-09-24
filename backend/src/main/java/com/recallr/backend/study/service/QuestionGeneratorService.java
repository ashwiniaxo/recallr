package com.recallr.backend.study.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallr.backend.ai.service.OllamaService;
import com.recallr.backend.study.dto.AnswerEvaluation;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.model.QuestionGoal;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.session.ActiveQuestion;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class QuestionGeneratorService {

    private static final int MAX_ATTEMPTS = 3;

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
                        new RuntimeException(
                                "Study material not found"
                        )
                );

        /*
         * Le type représente la FORME de la question.
         *
         * Exemples :
         * - MULTIPLE_CHOICE
         * - TRUE_FALSE
         * - SHORT_ANSWER
         *
         * Le goal représente le TYPE DE RAISONNEMENT
         * que l'on veut faire travailler.
         */
        QuestionGoal goal = selectQuestionGoal();

        String goalInstructions =
                getGoalInstructions(goal);

        String typeInstructions =
                switch (type) {

                    case MULTIPLE_CHOICE -> """
                            Create a high-quality multiple-choice question.

                            IMPORTANT:

                            - Provide exactly 4 answer options.

                            - Exactly ONE option must fully answer the question.

                            - The other 3 options must be plausible distractors.

                            - Distractors should represent realistic mistakes,
                              misunderstandings, confusions, or incomplete reasoning
                              that a student could make.

                            - Do NOT make the incorrect options obviously absurd.

                            - A student who does not understand the concept should
                              not be able to guess the answer simply because one
                              option looks much more detailed or sophisticated.

                            - Keep the answer options reasonably similar in style,
                              structure, and level of detail.

                            - Never create a question where multiple options are
                              individually correct.

                            - Never split a multi-part correct answer across several
                              options.

                            - If the answer contains several required elements,
                              put ALL required elements together in the single
                              correct option.

                            - All 4 options must be meaningfully different.

                            - Do not use duplicate options.

                            - Avoid "all of the above" and "none of the above".

                            - correctOptionIndex must identify the ONE fully
                              correct option.

                            - correctOptionIndex must be 0, 1, 2, or 3.

                            - correctAnswer must be null.

                            Whenever the cognitive goal allows it, prefer testing
                            understanding or reasoning instead of simple recognition.
                            """;

                    case TRUE_FALSE -> """
                            Create a high-quality True/False study statement.

                            The statement should test whether the student actually
                            understands an important idea from the study material.

                            Prefer statements involving:

                            - an important relationship between concepts;
                            - a consequence;
                            - an application;
                            - an important distinction;
                            - or a realistic misconception.

                            If the statement is false, prefer a realistic conceptual
                            mistake rather than an obviously absurd statement.

                            IMPORTANT:

                            - Write an AFFIRMATION, not a question.

                            - Do NOT ask "What is...?" or "Qu'est-ce que...?".

                            - Do NOT ask for a definition.

                            - Do NOT use a question mark.

                            - Avoid trivial statements that simply copy one sentence
                              word-for-word from the study material when a more
                              meaningful statement can be created.

                            - The statement must be unambiguously either true or
                              false according to the provided study material.

                            - options must be [].

                            - correctOptionIndex must be null.

                            - correctAnswer MUST be exactly "True" or "False".

                            - Never return null for correctAnswer.

                            Example of the expected JSON structure:

                            {
                              "question": "PEAS permet de décrire l'environnement de tâche d'un agent intelligent.",
                              "options": [],
                              "correctOptionIndex": null,
                              "correctAnswer": "True",
                              "explanation": "Explication pédagogique de la raison."
                            }
                            """;

                    case SHORT_ANSWER -> """
                            Create a high-quality open-ended short-answer question.

                            The student should normally be able to answer in
                            approximately 2 to 5 sentences.

                            Depending on the cognitive goal, prefer questions that
                            require the student to:

                            - explain why;
                            - explain how;
                            - apply a concept;
                            - interpret a situation;
                            - compare ideas;
                            - identify an error;
                            - justify a choice;
                            - explain a relationship;
                            - or reason about a short scenario.

                            For APPLICATION or ANALYSIS, prefer a short realistic
                            scenario when the provided study material contains
                            enough information to support one.

                            Do NOT require knowledge that is absent from the
                            study material.

                            Do NOT turn the question into a long essay question.

                            IMPORTANT:

                            - options must be [].

                            - correctOptionIndex must be null.

                            - correctAnswer must contain a strong expected answer.

                            - correctAnswer must never be null.

                            - The expected answer should contain the important ideas
                              needed to properly answer the question.

                            - Different wording from the expected answer should still
                              be considered valid later if the concepts are correct.
                            """;
                };

        String prompt = """
                You are an expert educational question designer for
                university students.

                Your objective is to create ONE high-quality study question
                that helps the student MASTER the concept.

                The goal is NOT merely to make the student memorize words.

                A good question should test meaningful knowledge,
                understanding, application, or reasoning depending on
                the requested cognitive goal.

                --------------------------------------------------
                CONCEPT
                --------------------------------------------------

                %s

                --------------------------------------------------
                STUDY MATERIAL
                --------------------------------------------------

                %s

                --------------------------------------------------
                QUESTION TYPE
                --------------------------------------------------

                %s

                --------------------------------------------------
                COGNITIVE GOAL
                --------------------------------------------------

                %s

                %s

                --------------------------------------------------
                QUESTION-TYPE INSTRUCTIONS
                --------------------------------------------------

                %s

                --------------------------------------------------
                GENERAL PEDAGOGICAL RULES
                --------------------------------------------------

                - Generate exactly ONE question.

                - Use ONLY information supported by the provided
                  study material.

                - Never require outside knowledge.

                - Focus on important ideas that are useful for mastering
                  the concept.

                - Avoid testing insignificant details unless those details
                  are important to understanding the concept.

                - Do not automatically ask for the definition of the
                  concept.

                - When appropriate, make the student reason about the
                  information instead of merely recognizing words from
                  the study material.

                - Difficulty should come from understanding and reasoning,
                  NOT from confusing wording.

                - The wording of the question must be clear.

                - Do not create trick questions.

                - Do not create intentionally ambiguous questions.

                - The expected answer must be defensible using the study
                  material.

                - When a scenario is used, it must be understandable using
                  only the provided material.

                - Prefer questions that would actually help a university
                  student prepare for an exam.

                --------------------------------------------------
                OUTPUT
                --------------------------------------------------

                Return exactly this JSON structure:

                {
                  "question": "question text",
                  "options": [],
                  "correctOptionIndex": null,
                  "correctAnswer": null,
                  "explanation": "clear educational explanation"
                }

                --------------------------------------------------
                JSON RULES
                --------------------------------------------------

                - Return ONLY valid JSON.

                - Never omit a field.

                - Do not add additional fields.

                - Do not use Markdown.

                - Detect the language used in the study material.

                - Write ALL user-facing text in that same language.

                - Never switch to another language.

                - If the study material is in French, the question,
                  options, expected answer, and explanation must all
                  be written in French.

                - JSON control values such as "True" and "False"
                  must remain exactly as requested.

                - The explanation should TEACH why the answer is correct,
                  not merely repeat the answer.
                """.formatted(
                material.getConcept(),
                material.getContent(),
                type,
                goal,
                goalInstructions,
                typeInstructions
        );

        Exception lastException = null;

        String currentPrompt = prompt;

        for (
                int attempt = 1;
                attempt <= MAX_ATTEMPTS;
                attempt++
        ) {

            try {

                System.out.println(
                        "Question generation attempt "
                                + attempt
                                + "/"
                                + MAX_ATTEMPTS
                                + " - material="
                                + materialId
                                + " - type="
                                + type
                                + " - goal="
                                + goal
                );

                String aiResponse =
                        ollamaService.generateJson(
                                currentPrompt
                        );

                System.out.println(aiResponse);

                GeneratedQuestion question =
                        parseGeneratedQuestion(
                                aiResponse,
                                type
                        );

                validateQuestion(
                        question,
                        type
                );

                return question;

            } catch (Exception e) {

                lastException = e;

                System.out.println(
                        "Question generation failed on attempt "
                                + attempt
                                + ": "
                                + e.getMessage()
                );

                currentPrompt = prompt + """

                        --------------------------------------------------
                        IMPORTANT CORRECTION
                        --------------------------------------------------

                        Your previous attempt was INVALID.

                        Validation error:

                        %s

                        Generate a DIFFERENT corrected question.

                        You MUST still follow:

                        Question type:
                        %s

                        Cognitive goal:
                        %s

                        Do not repeat the previous invalid structure.

                        Return ONLY the corrected JSON.
                        """.formatted(
                        e.getMessage(),
                        type,
                        goal
                );
            }
        }

        throw new RuntimeException(
                "Failed to generate a valid question after "
                        + MAX_ATTEMPTS
                        + " attempts",
                lastException
        );
    }

    /*
     * ------------------------------------------------------------
     * QUESTION GOAL
     * ------------------------------------------------------------
     *
     * RECALL        = 15 percent
     * UNDERSTANDING = 30 percent
     * APPLICATION   = 35 percent
     * ANALYSIS      = 20 percent
     *
     * On garde volontairement un peu de rappel,
     * mais Recallr favorise surtout la compréhension,
     * l'application et l'analyse.
     */

    private QuestionGoal selectQuestionGoal() {

        int random =
                ThreadLocalRandom.current()
                        .nextInt(100);

        if (random < 15) {
            return QuestionGoal.RECALL;
        }

        if (random < 45) {
            return QuestionGoal.UNDERSTANDING;
        }

        if (random < 80) {
            return QuestionGoal.APPLICATION;
        }

        return QuestionGoal.ANALYSIS;
    }

    private String getGoalInstructions(
            QuestionGoal goal
    ) {

        return switch (goal) {

            case RECALL -> """
                    RECALL GOAL:

                    Test an important piece of knowledge that the student
                    should remember.

                    This may include:
                    - an important definition;
                    - terminology;
                    - a key component;
                    - an important fact;
                    - or the purpose of a concept.

                    Even for recall, prefer meaningful knowledge over
                    insignificant details.
                    """;

            case UNDERSTANDING -> """
                    UNDERSTANDING GOAL:

                    Test whether the student truly understands the concept,
                    not merely whether they memorized its wording.

                    Prefer questions about:
                    - why something works;
                    - how something works;
                    - the purpose of something;
                    - relationships between ideas;
                    - consequences;
                    - differences between related ideas;
                    - or the meaning of the concept in the student's
                      own reasoning.

                    Avoid simple definition questions when possible.
                    """;

            case APPLICATION -> """
                    APPLICATION GOAL:

                    Require the student to APPLY the concept.

                    Prefer a short concrete example, situation, or scenario
                    when the study material provides enough information.

                    The student should need to determine how the concept
                    applies to that situation.

                    Do not require external domain knowledge.

                    The scenario should exist only to test the provided
                    study material.
                    """;

            case ANALYSIS -> """
                    ANALYSIS GOAL:

                    Require the student to reason about the concept.

                    Good analysis questions may ask the student to:
                    - identify a conceptual mistake;
                    - distinguish between related ideas;
                    - determine why a statement is correct or incorrect;
                    - analyze a situation;
                    - connect multiple ideas from the study material;
                    - identify the most appropriate explanation;
                    - or justify a conclusion.

                    Prefer realistic misconceptions when appropriate.

                    The question should be challenging because it requires
                    reasoning, not because it is vague or tricky.
                    """;
        };
    }

    /*
     * ------------------------------------------------------------
     * PARSE GENERATED QUESTION
     * ------------------------------------------------------------
     */

    private GeneratedQuestion parseGeneratedQuestion(
            String aiResponse,
            QuestionType type
    ) throws Exception {

        JsonNode json =
                objectMapper.readTree(aiResponse);

        if (json == null || !json.isObject()) {
            throw new IllegalArgumentException(
                    "AI response must be a JSON object"
            );
        }

        if (!json.has("question")) {
            throw new IllegalArgumentException(
                    "Question field is missing"
            );
        }

        if (!json.has("options")) {
            throw new IllegalArgumentException(
                    "Options field is missing"
            );
        }

        if (!json.has("correctOptionIndex")) {
            throw new IllegalArgumentException(
                    "Correct option index field is missing"
            );
        }

        if (!json.has("correctAnswer")) {
            throw new IllegalArgumentException(
                    "Correct answer field is missing"
            );
        }

        if (!json.has("explanation")) {
            throw new IllegalArgumentException(
                    "Explanation field is missing"
            );
        }

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
                json.get(
                        "correctOptionIndex"
                ).isNull()
                        ? null
                        : json.get(
                                "correctOptionIndex"
                        ).asInt();

        String correctAnswer =
                json.get(
                        "correctAnswer"
                ).isNull()
                        ? null
                        : json.get(
                                "correctAnswer"
                        ).asText();

        String questionText =
                json.get(
                        "question"
                ).asText();

        String explanation =
                json.get(
                        "explanation"
                ).isNull()
                        ? null
                        : json.get(
                                "explanation"
                        ).asText();

        if (type == QuestionType.TRUE_FALSE) {

            options = List.of(
                    "True",
                    "False"
            );

            correctOptionIndex = null;

            questionText =
                    normalizeTrueFalseStatement(
                            questionText
                    );
        }

        return new GeneratedQuestion(
                type,
                questionText,
                options,
                correctOptionIndex,
                correctAnswer,
                explanation
        );
    }

    /*
     * ------------------------------------------------------------
     * VALIDATION
     * ------------------------------------------------------------
     */

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

        if (
                question.question() == null ||
                question.question().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Question text is missing"
            );
        }

        if (
                question.explanation() == null ||
                question.explanation().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Explanation is missing"
            );
        }

        switch (expectedType) {

            case MULTIPLE_CHOICE -> {

                if (
                        question.options() == null ||
                        question.options().size() != 4
                ) {
                    throw new IllegalArgumentException(
                            "Multiple choice must contain exactly 4 options"
                    );
                }

                Set<String> distinctOptions =
                        new HashSet<>();

                for (
                        String option :
                        question.options()
                ) {

                    if (
                            option == null ||
                            option.isBlank()
                    ) {
                        throw new IllegalArgumentException(
                                "Multiple choice options cannot be blank"
                        );
                    }

                    distinctOptions.add(
                            option.trim()
                                    .toLowerCase()
                    );
                }

                if (distinctOptions.size() != 4) {
                    throw new IllegalArgumentException(
                            "Multiple choice options must all be different"
                    );
                }

                if (
                        question.correctOptionIndex() == null
                ) {
                    throw new IllegalArgumentException(
                            "Correct option index is missing"
                    );
                }

                if (
                        question.correctOptionIndex() < 0 ||
                        question.correctOptionIndex() >= 4
                ) {
                    throw new IllegalArgumentException(
                            "Correct option index must be between 0 and 3"
                    );
                }

                if (
                        question.correctAnswer() != null
                ) {
                    throw new IllegalArgumentException(
                            "Multiple choice correctAnswer must be null"
                    );
                }
            }

            case TRUE_FALSE -> {

                if (
                        question.question()
                                .contains("?")
                ) {
                    throw new IllegalArgumentException(
                            "True/False must be a statement, not a question"
                    );
                }

                if (
                        question.options() == null ||
                        question.options().size() != 2 ||
                        !question.options().contains("True") ||
                        !question.options().contains("False")
                ) {
                    throw new IllegalArgumentException(
                            "True/False options must be True and False"
                    );
                }

                if (
                        question.correctOptionIndex() != null
                ) {
                    throw new IllegalArgumentException(
                            "True/False correctOptionIndex must be null"
                    );
                }

                if (
                        question.correctAnswer() == null ||
                        (
                                !question.correctAnswer()
                                        .equalsIgnoreCase("True") &&
                                !question.correctAnswer()
                                        .equalsIgnoreCase("False")
                        )
                ) {
                    throw new IllegalArgumentException(
                            "True/False answer must be True or False"
                    );
                }
            }

            case SHORT_ANSWER -> {

                if (
                        question.options() == null ||
                        !question.options().isEmpty()
                ) {
                    throw new IllegalArgumentException(
                            "Short answer options must be empty"
                    );
                }

                if (
                        question.correctOptionIndex() != null
                ) {
                    throw new IllegalArgumentException(
                            "Short answer correctOptionIndex must be null"
                    );
                }

                if (
                        question.correctAnswer() == null ||
                        question.correctAnswer().isBlank()
                ) {
                    throw new IllegalArgumentException(
                            "Short answer correctAnswer is missing"
                    );
                }
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * SHORT ANSWER EVALUATION
     * ------------------------------------------------------------
     */

    public AnswerEvaluation evaluateShortAnswer(
            StudyMaterial material,
            ActiveQuestion question,
            String userAnswer
    ) {

        String prompt = """
                You are an expert educational tutor evaluating a
                university student's answer.

                Your PRIMARY objective is to help the student MASTER
                the concept.

                Grading is important, but teaching the student after
                the answer is equally important.

                Evaluate the student's answer using ONLY the provided
                study material.

                --------------------------------------------------
                CONCEPT
                --------------------------------------------------

                %s

                --------------------------------------------------
                STUDY MATERIAL
                --------------------------------------------------

                %s

                --------------------------------------------------
                QUESTION
                --------------------------------------------------

                %s

                --------------------------------------------------
                EXPECTED ANSWER
                --------------------------------------------------

                %s

                --------------------------------------------------
                STUDENT ANSWER
                --------------------------------------------------

                %s

                --------------------------------------------------
                EVALUATION PRINCIPLES
                --------------------------------------------------

                Evaluate conceptual understanding, NOT exact wording.

                The student may explain something using different words
                and still be completely correct.

                Give appropriate credit for every important idea that
                the student correctly understands.

                Do not penalize the student simply because their answer
                is shorter than the expected answer if it still answers
                the question correctly.

                Do not require details that the QUESTION did not
                reasonably ask for.

                Do not require information that is absent from the
                provided study material.

                Distinguish carefully between:

                1. CORRECT CONCEPTS
                   Things the student correctly understood or explained.

                2. MISSING CONCEPTS
                   Important information that should have been included
                   to answer the question more completely.

                3. INCORRECT CONCEPTS
                   Actual misconceptions, factual errors, or incorrect
                   reasoning in the student's answer.

                Missing information is NOT automatically an incorrect
                concept.

                --------------------------------------------------
                FEEDBACK OBJECTIVE
                --------------------------------------------------

                After evaluating the answer:

                1. Explain what the student understood correctly.

                2. Identify important information that is missing.

                3. Identify actual misconceptions or incorrect statements.

                4. Explain the correct reasoning clearly.

                5. Explain how the student could improve their answer.

                6. Provide a strong complete expected answer.

                The feedback should help the student LEARN from the
                question, not simply tell them whether they were right
                or wrong.

                Even when the answer receives the maximum score,
                provide a useful explanation reinforcing WHY the answer
                is correct.

                However, do not add unnecessary information merely to
                make the feedback longer.

                --------------------------------------------------
                SCORING
                --------------------------------------------------

                Use the following general interpretation:

                90-100:
                Excellent understanding. The answer correctly addresses
                essentially all important aspects required by the question.

                70-89:
                Good understanding. The main idea is correct, but some
                useful or important details may be missing.

                40-69:
                Partial understanding. Some important ideas are correct,
                but significant information is missing or confused.

                1-39:
                Very limited understanding. Only a small part of the
                required concept is understood correctly.

                0:
                The answer does not demonstrate correct understanding
                relevant to the question.

                correct should normally be true when score is 70 or higher.

                --------------------------------------------------
                OUTPUT
                --------------------------------------------------

                Return exactly this JSON structure:

                {
                  "score": 0,
                  "correct": false,
                  "feedback": "overall educational feedback",
                  "explanation": "clear explanation of the concept and correct reasoning",
                  "expectedAnswer": "example of a strong complete answer",
                  "correctConcepts": [],
                  "missingConcepts": [],
                  "incorrectConcepts": [],
                  "howToImprove": "specific advice for improving the answer"
                }

                --------------------------------------------------
                JSON RULES
                --------------------------------------------------

                - Return ONLY valid JSON.

                - Never omit a field.

                - Do not add additional fields.

                - score must be between 0 and 100.

                - correct should normally be true when score is
                  70 or higher.

                - correctConcepts must contain concise descriptions
                  of ideas the student correctly demonstrated.

                - missingConcepts must contain only important ideas
                  that were genuinely missing.

                - incorrectConcepts must contain actual mistakes or
                  misconceptions.

                - If there are no missing concepts, return [].

                - If there are no incorrect concepts, return [].

                - howToImprove should give concrete advice based on
                  this specific answer.

                - expectedAnswer should be a good example answer to
                  THIS specific question.

                - Do not require exact wording.

                - Do not introduce knowledge outside the study material.

                - Use only the provided study material.

                - Detect the language of the study material.

                - Write ALL user-facing feedback in that same language.

                - If the study material is in French, all feedback,
                  explanations, concepts, improvement advice, and the
                  expected answer must be in French.

                - Do not switch languages.
                """.formatted(
                material.getConcept(),
                material.getContent(),
                question.question(),
                question.correctAnswer(),
                userAnswer
        );

        String json =
                ollamaService.generateJson(
                        prompt
                );

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

    /*
     * ------------------------------------------------------------
     * TRUE / FALSE NORMALIZATION
     * ------------------------------------------------------------
     */

    private String normalizeTrueFalseStatement(
            String text
    ) {

        if (text == null) {
            return null;
        }

        String normalized =
                text.trim();

        normalized =
                normalized.replaceAll(
                        "(?i)\\s*(c['’]est|est-ce)\\s+"
                                + "(vrai\\s+ou\\s+faux|vrai)"
                                + "\\s*\\?\\s*$",
                        ""
                );

        normalized =
                normalized.replaceAll(
                        "(?i)\\s*(vrai\\s+ou\\s+faux)"
                                + "\\s*\\?\\s*$",
                        ""
                );

        normalized =
                normalized.replaceAll(
                        "(?i)\\s*(true\\s+or\\s+false)"
                                + "\\s*\\?\\s*$",
                        ""
                );

        return normalized.trim();
    }
}