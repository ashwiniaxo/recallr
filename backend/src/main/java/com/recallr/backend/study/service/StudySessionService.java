package com.recallr.backend.study.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.recallr.backend.progress.service.ProgressService;
import com.recallr.backend.study.dto.AnswerEvaluation;
import com.recallr.backend.study.dto.AnswerRequest;
import com.recallr.backend.study.dto.AnswerResult;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.dto.StudyQuestion;
import com.recallr.backend.study.dto.StudySessionRequest;
import com.recallr.backend.study.dto.StudySessionResponse;
import com.recallr.backend.study.dto.StudySessionStatusResponse;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.session.ActiveQuestion;
import com.recallr.backend.study.session.ActiveStudySession;
import com.recallr.backend.study.session.StudySessionStore;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class StudySessionService {

    /*
     * Évite une boucle infinie si Qwen n'arrive
     * vraiment pas à générer certaines questions.
     *
     * Exemple :
     * 5 questions demandées
     * -> maximum 25 générations complètes.
     *
     * Chaque génération possède elle-même jusqu'à
     * 3 essais dans QuestionGeneratorService.
     */
    private static final int SESSION_ATTEMPT_MULTIPLIER = 5;

    private final StudyMaterialRepository studyMaterialRepository;
    private final QuestionGeneratorService questionGeneratorService;
    private final StudySessionStore sessionStore;
    private final ProgressService progressService;
    private final AdaptiveStudyService adaptiveStudyService;

    public StudySessionService(
            StudyMaterialRepository studyMaterialRepository,
            QuestionGeneratorService questionGeneratorService,
            StudySessionStore sessionStore,
            ProgressService progressService,
            AdaptiveStudyService adaptiveStudyService
    ) {
        this.studyMaterialRepository =
                studyMaterialRepository;

        this.questionGeneratorService =
                questionGeneratorService;

        this.sessionStore =
                sessionStore;

        this.progressService =
                progressService;

        this.adaptiveStudyService =
                adaptiveStudyService;
    }

    public StudySessionResponse createSession(
            Long sectionId,
            StudySessionRequest request
    ) {

        if (request.questionCount() <= 0) {
            throw new IllegalArgumentException(
                    "Question count must be greater than 0"
            );
        }

        if (request.questionTypes() == null ||
                request.questionTypes().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one question type is required"
            );
        }

        List<StudyMaterial> materials =
                studyMaterialRepository
                        .findByCourseSectionId(
                                sectionId
                        );

        if (materials.isEmpty()) {
            throw new RuntimeException(
                    "No study materials found for this section"
            );
        }

        List<StudyMaterial> prioritizedMaterials =
                adaptiveStudyService
                        .prioritizeMaterials(
                                materials
                        );

        String sessionId =
                UUID.randomUUID().toString();

        List<ActiveQuestion> activeQuestions =
                new ArrayList<>();

        List<StudyQuestion> publicQuestions =
                new ArrayList<>();

        /*
         * Nombre maximum de générations complètes
         * autorisées pour construire cette session.
         */
        int maxGenerationAttempts =
                Math.max(
                        request.questionCount()
                                * SESSION_ATTEMPT_MULTIPLIER,
                        request.questionCount()
                );

        int generationAttempts = 0;
        int materialIndex = 0;

        while (
                publicQuestions.size()
                        < request.questionCount() &&
                generationAttempts
                        < maxGenerationAttempts
        ) {

            StudyMaterial material =
                    prioritizedMaterials.get(
                            materialIndex
                                    % prioritizedMaterials.size()
                    );

            materialIndex++;
            generationAttempts++;

            QuestionType type =
                    request.questionTypes().get(
                            ThreadLocalRandom
                                    .current()
                                    .nextInt(
                                            request.questionTypes()
                                                    .size()
                                    )
                    );

            System.out.println(
                    "Session generation attempt "
                            + generationAttempts
                            + "/"
                            + maxGenerationAttempts
                            + " - question "
                            + (publicQuestions.size() + 1)
                            + "/"
                            + request.questionCount()
                            + " - material="
                            + material.getId()
                            + " - type="
                            + type
            );

            try {

                GeneratedQuestion generated =
                        questionGeneratorService
                                .generateQuestion(
                                        material.getId(),
                                        type
                                );

                String questionId =
                        UUID.randomUUID()
                                .toString();

                ActiveQuestion activeQuestion =
                        new ActiveQuestion(
                                questionId,
                                material.getId(),
                                generated.type(),
                                generated.question(),
                                generated.options(),
                                generated.correctOptionIndex(),
                                generated.correctAnswer(),
                                generated.explanation()
                        );

                activeQuestions.add(
                        activeQuestion
                );

                /*
                 * questionNumber dépend maintenant
                 * du nombre de questions réellement
                 * générées avec succès.
                 */
                int questionNumber =
                        publicQuestions.size() + 1;

                StudyQuestion publicQuestion =
                        new StudyQuestion(
                                questionId,
                                questionNumber,
                                material.getId(),
                                generated.type(),
                                generated.question(),
                                generated.options()
                        );

                publicQuestions.add(
                        publicQuestion
                );

            } catch (RuntimeException e) {

                /*
                 * Une génération invalide ne détruit
                 * plus toute la session.
                 */
                System.out.println(
                        "Skipping failed question generation"
                );

                System.out.println(
                        "Material: "
                                + material.getId()
                                + ", type: "
                                + type
                );

                System.out.println(
                        "Reason: "
                                + e.getMessage()
                );
            }
        }

        /*
         * Même après plusieurs concepts/types différents,
         * Recallr n'a pas réussi à obtenir suffisamment
         * de questions valides.
         *
         * On ne crée pas une session incomplète.
         */
        if (publicQuestions.size()
                < request.questionCount()) {

            throw new RuntimeException(
                    "Unable to generate enough valid questions. "
                            + "Requested: "
                            + request.questionCount()
                            + ", generated: "
                            + publicQuestions.size()
                            + ", attempts: "
                            + generationAttempts
            );
        }

        ActiveStudySession activeSession =
                new ActiveStudySession(
                        sessionId,
                        sectionId,
                        activeQuestions
                );

        sessionStore.save(
                activeSession
        );

        return new StudySessionResponse(
                sessionId,
                sectionId,
                publicQuestions.size(),
                publicQuestions
        );
    }

    public AnswerResult answerQuestion(
            String sessionId,
            String questionId,
            AnswerRequest request
    ) {

        ActiveStudySession session =
                sessionStore
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study session not found"
                                )
                        );

        ActiveQuestion question =
                session.findQuestion(
                        questionId
                );

        if (question.isAnswered()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This question has already been answered"
            );
        }

        /*
         * On évalue d'abord.
         *
         * Si Qwen échoue pendant une SHORT_ANSWER,
         * la question n'est donc PAS marquée comme répondue.
         */
        AnswerResult result =
                switch (question.type()) {

                    case MULTIPLE_CHOICE ->
                            evaluateMultipleChoice(
                                    question,
                                    request
                            );

                    case TRUE_FALSE ->
                            evaluateTrueFalse(
                                    question,
                                    request
                            );

                    case SHORT_ANSWER ->
                            evaluateShortAnswer(
                                    question,
                                    request
                            );
                };

        /*
         * Seulement après une évaluation réussie.
         */
        question.markAnswered(
                result.score(),
                result.correct()
        );

        progressService.recordAnswer(
                question.materialId(),
                result.score(),
                result.correct()
        );

        return result;
    }

    private AnswerResult evaluateMultipleChoice(
            ActiveQuestion question,
            AnswerRequest request
    ) {

        if (request.selectedOptionIndex() == null) {
            throw new IllegalArgumentException(
                    "selectedOptionIndex is required"
            );
        }

        if (request.selectedOptionIndex() < 0 ||
                request.selectedOptionIndex()
                        >= question.options().size()) {

            throw new IllegalArgumentException(
                    "Invalid option index"
            );
        }

        boolean correct =
                request.selectedOptionIndex()
                        .equals(
                                question.correctOptionIndex()
                        );

        String expectedAnswer =
                question.options().get(
                        question.correctOptionIndex()
                );

                return new AnswerResult(
                        correct,
                        correct ? 100 : 0,
                        correct
                                ? "Correct!"
                                : "Incorrect.",
                        question.explanation(),
                        expectedAnswer,
                        List.of(),
                        List.of(),
                        List.of(),
                        ""
                );
    }

    private AnswerResult evaluateTrueFalse(
            ActiveQuestion question,
            AnswerRequest request
    ) {

        if (request.answer() == null ||
                request.answer().isBlank()) {

            throw new IllegalArgumentException(
                    "answer is required"
            );
        }

        String userAnswer =
                request.answer().trim();

        if (!userAnswer.equalsIgnoreCase("True") &&
                !userAnswer.equalsIgnoreCase("False")) {

            throw new IllegalArgumentException(
                    "Answer must be True or False"
            );
        }

        boolean correct =
                userAnswer.equalsIgnoreCase(
                        question.correctAnswer()
                );

        return new AnswerResult(
                correct,
                correct ? 100 : 0,
                correct
                        ? "Correct!"
                        : "Incorrect.",
                question.explanation(),
                question.correctAnswer(),
                List.of(),
                List.of(),
                List.of(),
                ""
        );
    }

    private AnswerResult evaluateShortAnswer(
            ActiveQuestion question,
            AnswerRequest request
    ) {

        if (request.answer() == null ||
                request.answer().isBlank()) {

            throw new IllegalArgumentException(
                    "answer is required"
            );
        }

        StudyMaterial material =
                studyMaterialRepository
                        .findById(
                                question.materialId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study material not found"
                                )
                        );

        AnswerEvaluation evaluation =
                questionGeneratorService
                        .evaluateShortAnswer(
                                material,
                                question,
                                request.answer()
                        );

        return new AnswerResult(
                evaluation.correct(),
                evaluation.score(),
                evaluation.feedback(),
                evaluation.explanation(),
                evaluation.expectedAnswer(),
                evaluation.correctConcepts(),
                evaluation.missingConcepts(),
                evaluation.incorrectConcepts(),
                evaluation.howToImprove()
        );
    }

    public StudySessionStatusResponse getSessionStatus(
            String sessionId
    ) {

        ActiveStudySession session =
                sessionStore
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study session not found"
                                )
                        );

        return new StudySessionStatusResponse(
                session.getSessionId(),
                session.getSectionId(),
                session.getTotalQuestions(),
                session.getAnsweredQuestions(),
                session.getRemainingQuestions(),
                session.getCorrectAnswers(),
                session.getIncorrectAnswers(),
                session.getAverageScore(),
                session.isCompleted()
        );
    }
}