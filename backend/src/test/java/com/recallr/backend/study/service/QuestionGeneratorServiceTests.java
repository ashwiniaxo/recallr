package com.recallr.backend.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.recallr.backend.ai.service.OllamaService;
import com.recallr.backend.study.dto.AnswerEvaluation;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.session.ActiveQuestion;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

class QuestionGeneratorServiceTests {

    private final OllamaService ollamaService = mock(OllamaService.class);
    private final StudyMaterialRepository materialRepository = mock(StudyMaterialRepository.class);
    private final QuestionGeneratorService service = new QuestionGeneratorService(ollamaService, materialRepository);
    private final StudyMaterial material = new StudyMaterial();

    @BeforeEach
    void prepareMaterial() {
        material.setConcept("Renforcement");
        material.setContent("Un renforcement augmente la fréquence d'un comportement.");
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class, names = {"SHORT_ANSWER", "CASE_STUDY", "ERROR_ANALYSIS", "COMPARISON"})
    void generatesEachWrittenFormatWithAWorkedAnswer(QuestionType type) {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(questionJson("Justifie la décision dans ce cas."));

        GeneratedQuestion question = service.generateQuestion(1L, type);

        assertThat(question.type()).isEqualTo(type);
        assertThat(question.options()).isEmpty();
        assertThat(question.correctOptionIndex()).isNull();
        assertThat(question.correctAnswer()).isNotBlank();
        if (type != QuestionType.SHORT_ANSWER) {
            assertThat(question.question()).contains("1. Justifie", "2. Explique");
        }
    }

    @Test
    void rejectsDuplicatesWithoutStartingAnotherModelCall() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema()))
                .thenReturn(questionJson("Justifie cette décision."));
        assertThatThrownBy(() -> service.generateQuestion(1L, QuestionType.CASE_STUDY,
                List.of("Justifie cette décision.\n\n1. Justifie ton application.\n2. Explique une conséquence.")))
                .hasMessageContaining("Failed to generate a valid question");
        verify(ollamaService, times(1)).generateStructuredJson(anyString(), nonReviewSchema());
    }

    @Test
    void generatesAValidQuestionWithOnlyOneModelCall() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema()))
                .thenReturn(questionJson("Une situation."));
        service.generateQuestion(1L, QuestionType.CASE_STUDY);
        verify(ollamaService, times(1)).generateStructuredJson(anyString(), nonReviewSchema());
        org.mockito.Mockito.verifyNoMoreInteractions(ollamaService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\""})
    void rejectsWrittenQuestionsWithoutAnExpectedAnswer(String answer) {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(
                questionJson("Analyse ce cas.").replace("\"Une réponse complète et justifiée.\"", answer)
        );

        assertThatThrownBy(() -> service.generateQuestion(1L, QuestionType.ERROR_ANALYSIS))
                .hasMessageContaining("Failed to generate a valid question");
        verify(ollamaService, times(1)).generateStructuredJson(anyString(), nonReviewSchema());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[\"Une seule tâche\"]", "[\"Justifie\",\"\"]"})
    void rejectsReasoningExercisesWithoutExplicitNonblankTasks(String tasks) {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(
                questionJson("Applique ce concept.").replace(
                        "[\"Justifie ton application.\",\"Explique une conséquence.\"]", tasks)
        );

        assertThatThrownBy(() -> service.generateQuestion(1L, QuestionType.CASE_STUDY))
                .hasMessageContaining("Failed to generate a valid question");
    }

    @Test
    void keepsExistingMultipleChoiceAndTrueFalseFormatsWorking() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn("""
                {"question":"Quelle décision convient ?","options":["A","B","C","D"],
                 "correctOptionIndex":2,"correctAnswer":null,"explanation":"C est justifié."}
                """, """
                {"question":"Cette conséquence est cohérente.","options":[],
                 "correctOptionIndex":null,"correctAnswer":"False","explanation":"La conséquence est inversée."}
                """);

        assertThat(service.generateQuestion(1L, QuestionType.MULTIPLE_CHOICE).correctOptionIndex()).isEqualTo(2);
        assertThat(service.generateQuestion(1L, QuestionType.TRUE_FALSE).options()).containsExactly("True", "False");
    }

    @ParameterizedTest
    @CsvSource({"84,true,false", "85,false,true", "100,true,true"})
    void enforcesTheWrittenAnswerSuccessThreshold(int score, boolean aiCorrect, boolean expectedCorrect) {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(evaluationJson(score, aiCorrect, "[]", "[]"));

        AnswerEvaluation evaluation = service.evaluateWrittenAnswer(material, activeQuestion(), "Mon raisonnement.");

        assertThat(evaluation.score()).isEqualTo(score);
        assertThat(evaluation.correct()).isEqualTo(expectedCorrect);
    }

    @Test
    void doesNotAcceptAnAnswerWithMissingEssentialReasoningEvenIfAiAwardsFullMarks() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(
                evaluationJson(100, true, "[\"Justifier la conséquence dans le cas présenté\"]", "[]")
        );

        AnswerEvaluation evaluation = service.evaluateWrittenAnswer(material, activeQuestion(), "Une définition.");

        assertThat(evaluation.score()).isEqualTo(84);
        assertThat(evaluation.correct()).isFalse();
        assertThat(evaluation.missingConcepts()).hasSize(1);
    }

    @Test
    void preservesPartialCreditButRejectsConceptualErrors() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(
                evaluationJson(95, true, "[]", "[\"La conséquence est inversée\"]")
        );

        AnswerEvaluation evaluation = service.evaluateWrittenAnswer(material, activeQuestion(), "Un raisonnement erroné.");

        assertThat(evaluation.score()).isEqualTo(69);
        assertThat(evaluation.correct()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void rejectsOutOfRangeScores(int score) {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn(evaluationJson(score, true, "[]", "[]"));

        assertThatThrownBy(() -> service.evaluateWrittenAnswer(material, activeQuestion(), "Ma réponse."))
                .hasMessage("Failed to parse AI answer evaluation");
    }

    @Test
    void rejectsIncompleteEvaluationInsteadOfRecordingAScore() {
        when(ollamaService.generateStructuredJson(anyString(), nonReviewSchema())).thenReturn("{\"score\":100,\"correct\":true}");

        assertThatThrownBy(() -> service.evaluateWrittenAnswer(material, activeQuestion(), "Ma réponse."))
                .hasMessage("Failed to parse AI answer evaluation");
    }

    private ActiveQuestion activeQuestion() {
        return new ActiveQuestion("question", 1L, QuestionType.CASE_STUDY,
                "Applique la notion et justifie la conséquence.", List.of(), null,
                "Une application avec justification.", "Le raisonnement attendu.");
    }

    private Map<String, Object> nonReviewSchema() {
        return argThat(schema -> schema != null && !((Map<?, ?>) schema.get("properties")).containsKey("valid"));
    }

    private Map<String, Object> reviewSchema() {
        return argThat(schema -> schema != null && ((Map<?, ?>) schema.get("properties")).containsKey("valid"));
    }

    private String questionJson(String question) {
        return """
                {"question":"%s","options":[],"correctOptionIndex":null,
                 "tasks":["Justifie ton application.","Explique une conséquence."],
                 "correctAnswer":"Une réponse complète et justifiée.","explanation":"Le raisonnement détaillé."}
                """.formatted(question);
    }

    private String evaluationJson(int score, boolean correct, String missing, String incorrect) {
        return """
                {"score":%d,"correct":%s,"feedback":"Bilan de la réponse.",
                 "explanation":"Raisonnement détaillé.","expectedAnswer":"Réponse justifiée.",
                 "correctConcepts":["Principe identifié"],"missingConcepts":%s,"incorrectConcepts":%s,
                 "howToImprove":"Relier le principe aux faits du cas."}
                """.formatted(score, correct, missing, incorrect);
    }
}
