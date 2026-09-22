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
        this.studyMaterialRepository = studyMaterialRepository;
        this.questionGeneratorService = questionGeneratorService;
        this.sessionStore = sessionStore;
        this.progressService = progressService;
        this.adaptiveStudyService = adaptiveStudyService;
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
                studyMaterialRepository.findByCourseSectionId(sectionId);

        if (materials.isEmpty()) {
            throw new RuntimeException(
                    "No study materials found for this section"
            );
        }

        List<StudyMaterial> prioritizedMaterials =
                adaptiveStudyService.prioritizeMaterials(
                        materials
                );

        String sessionId = UUID.randomUUID().toString();

        List<ActiveQuestion> activeQuestions =
                new ArrayList<>();

        List<StudyQuestion> publicQuestions =
                new ArrayList<>();

        for (int i = 0; i < request.questionCount(); i++) {

        StudyMaterial material =
                prioritizedMaterials.get(
                        i % prioritizedMaterials.size()
                );

            QuestionType type =
                    request.questionTypes().get(
                            ThreadLocalRandom.current().nextInt(
                                    request.questionTypes().size()
                            )
                    );

            GeneratedQuestion generated =
                    questionGeneratorService.generateQuestion(
                            material.getId(),
                            type
                    );

            String questionId =
                    UUID.randomUUID().toString();

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

            activeQuestions.add(activeQuestion);

            StudyQuestion publicQuestion =
                    new StudyQuestion(
                            questionId,
                            i + 1,
                            material.getId(),
                            generated.type(),
                            generated.question(),
                            generated.options()
                    );

            publicQuestions.add(publicQuestion);
        }

        ActiveStudySession activeSession =
                new ActiveStudySession(
                        sessionId,
                        sectionId,
                        activeQuestions
                );

        sessionStore.save(activeSession);

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
                sessionStore.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study session not found"
                                )
                        );

        ActiveQuestion question =
                session.findQuestion(questionId);

        if (question.isAnswered()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This question has already been answered"
                );
        }

        AnswerResult result = switch (question.type()) {

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
                request.selectedOptionIndex() >=
                        question.options().size()) {

                throw new IllegalArgumentException(
                        "Invalid option index"
                );
        }

        boolean correct =
                request.selectedOptionIndex()
                        .equals(question.correctOptionIndex());

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
                List.of()
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
                List.of()
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
                        .findById(question.materialId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study material not found"
                                )
                        );

        AnswerEvaluation evaluation =
                questionGeneratorService.evaluateShortAnswer(
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
                evaluation.missingConcepts()
        );
        }

        public StudySessionStatusResponse getSessionStatus(
                String sessionId
        ) {

        ActiveStudySession session =
                sessionStore.findById(sessionId)
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