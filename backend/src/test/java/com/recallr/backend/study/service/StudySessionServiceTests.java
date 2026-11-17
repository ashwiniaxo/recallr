package com.recallr.backend.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.recallr.backend.progress.service.ProgressService;
import com.recallr.backend.study.dto.AnswerEvaluation;
import com.recallr.backend.study.dto.AnswerRequest;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.dto.StudyQuestion;
import com.recallr.backend.study.dto.StudySessionRequest;
import com.recallr.backend.study.dto.StudySessionResponse;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.session.StudySessionStore;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

class StudySessionServiceTests {

    private final StudyMaterialRepository materialRepository = mock(StudyMaterialRepository.class);
    private final QuestionGeneratorService generator = mock(QuestionGeneratorService.class);
    private final ProgressService progress = mock(ProgressService.class);
    private final AdaptiveStudyService adaptiveStudy = mock(AdaptiveStudyService.class);
    private final StudySessionStore store = new StudySessionStore();
    private final StudySessionService service = new StudySessionService(
            materialRepository, generator, store, progress, adaptiveStudy
    );
    private final StudyMaterial material = mock(StudyMaterial.class);

    @BeforeEach
    void prepareGeneration() {
        when(material.getId()).thenReturn(1L);
        when(materialRepository.findByCourseSectionId(2L)).thenReturn(List.of(material));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(adaptiveStudy.prioritizeMaterials(anyList())).thenReturn(List.of(material));
        AtomicInteger count = new AtomicInteger();
        when(generator.generateQuestion(eq(1L), any(QuestionType.class), anyList())).thenAnswer(invocation ->
                new GeneratedQuestion(invocation.getArgument(1), "Cas " + count.incrementAndGet(),
                        List.of(), null, "Réponse justifiée", "Explication")
        );
    }

    @Test
    void includesEverySelectedFormatBeforeRepeatingOneAndPassesPreviousQuestions() {
        List<QuestionType> types = List.of(QuestionType.CASE_STUDY, QuestionType.ERROR_ANALYSIS, QuestionType.COMPARISON);

        StudySessionResponse session = service.createSession(2L, new StudySessionRequest(6, types));

        assertThat(session.questions().subList(0, 3)).extracting(StudyQuestion::type)
                .containsExactlyInAnyOrderElementsOf(types);
        assertThat(session.questions().subList(3, 6)).extracting(StudyQuestion::type)
                .containsExactlyInAnyOrderElementsOf(types);
        verify(generator).generateQuestion(eq(1L), any(QuestionType.class), eq(List.of("Cas 1", "Cas 2")));
    }

    @Test
    void returnsAvailableQuestionsAndBoundsFailures() {
        AtomicInteger calls = new AtomicInteger();
        when(generator.generateQuestion(eq(1L), any(QuestionType.class), anyList())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() > 1) throw new RuntimeException("Invalid model output");
            return new GeneratedQuestion(QuestionType.SHORT_ANSWER, "Une question valide",
                    List.of(), null, "Réponse", "Explication");
        });
        var session = service.createSession(2L, new StudySessionRequest(5, List.of(QuestionType.SHORT_ANSWER)));
        assertThat(session.questionCount()).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(7);
        assertThat(store.findById(session.sessionId())).isPresent();
    }

    @Test
    void doesNotSendOtherConceptsQuestionsToTheModel() {
        StudyMaterial other = mock(StudyMaterial.class);
        when(other.getId()).thenReturn(3L);
        when(adaptiveStudy.prioritizeMaterials(anyList())).thenReturn(List.of(material, other));
        when(generator.generateQuestion(eq(3L), any(), anyList())).thenReturn(
                new GeneratedQuestion(QuestionType.SHORT_ANSWER, "Autre concept", List.of(), null, "Réponse", "Explication"));
        service.createSession(2L, new StudySessionRequest(2, List.of(QuestionType.SHORT_ANSWER)));
        verify(generator).generateQuestion(eq(3L), any(), eq(List.of()));
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = {"SHORT_ANSWER", "CASE_STUDY", "ERROR_ANALYSIS", "COMPARISON"})
    void evaluatesEachWrittenFormatAndRecordsItsResult(QuestionType type) {
        StudySessionResponse session = service.createSession(2L, new StudySessionRequest(1, List.of(type)));
        StudyQuestion question = session.questions().getFirst();
        when(generator.evaluateWrittenAnswer(eq(material), any(), eq("Mon raisonnement."))).thenReturn(
                new AnswerEvaluation(90, true, "Bien justifié", "Explication", "Réponse complète",
                        List.of("Application"), List.of(), List.of(), "Approfondir la conséquence")
        );

        var result = service.answerQuestion(session.sessionId(), question.questionId(), new AnswerRequest(null, "Mon raisonnement."));

        assertThat(result.score()).isEqualTo(90);
        verify(progress).recordAnswer(1L, 90, true);
        assertThat(service.getSessionStatus(session.sessionId()).completed()).isTrue();
    }

    @Test
    void leavesQuestionUnansweredWhenAiEvaluationFailsSoStudentCanRetry() {
        StudySessionResponse session = service.createSession(2L,
                new StudySessionRequest(1, List.of(QuestionType.ERROR_ANALYSIS)));
        StudyQuestion question = session.questions().getFirst();
        when(generator.evaluateWrittenAnswer(eq(material), any(), any()))
                .thenThrow(new RuntimeException("Invalid AI evaluation"));

        assertThatThrownBy(() -> service.answerQuestion(session.sessionId(), question.questionId(),
                new AnswerRequest(null, "Ma correction."))).hasMessage("Invalid AI evaluation");

        assertThat(service.getSessionStatus(session.sessionId()).answeredQuestions()).isZero();
        verify(progress, never()).recordAnswer(any(), anyInt(), anyBoolean());
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = {"SHORT_ANSWER", "CASE_STUDY", "ERROR_ANALYSIS", "COMPARISON"})
    void rejectsBlankWrittenAnswersBeforeCallingTheAi(QuestionType type) {
        StudySessionResponse session = service.createSession(2L, new StudySessionRequest(1, List.of(type)));

        assertThatThrownBy(() -> service.answerQuestion(session.sessionId(), session.questions().getFirst().questionId(),
                new AnswerRequest(null, "   "))).hasMessage("answer is required");

        verify(generator, never()).evaluateWrittenAnswer(any(), any(), any());
    }
}
