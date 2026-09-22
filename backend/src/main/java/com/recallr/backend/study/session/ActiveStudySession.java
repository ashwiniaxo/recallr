package com.recallr.backend.study.session;

import java.time.LocalDateTime;
import java.util.List;

public class ActiveStudySession {

    private final String sessionId;
    private final Long sectionId;
    private final LocalDateTime startedAt;
    private final List<ActiveQuestion> questions;

    public ActiveStudySession(
            String sessionId,
            Long sectionId,
            List<ActiveQuestion> questions
    ) {
        this.sessionId = sessionId;
        this.sectionId = sectionId;
        this.questions = questions;
        this.startedAt = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public List<ActiveQuestion> getQuestions() {
        return questions;
    }

    public ActiveQuestion findQuestion(String questionId) {
        return questions.stream()
                .filter(question ->
                        question.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Question not found in this session"
                        )
                );
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public int getAnsweredQuestions() {
        return (int) questions.stream()
                .filter(ActiveQuestion::isAnswered)
                .count();
    }

    public int getRemainingQuestions() {
        return getTotalQuestions() - getAnsweredQuestions();
    }

    public boolean isCompleted() {
        return getTotalQuestions() > 0
                && getAnsweredQuestions() == getTotalQuestions();
    }

    public int getCorrectAnswers() {
    return (int) questions.stream()
            .filter(ActiveQuestion::isAnswered)
            .filter(question ->
                    Boolean.TRUE.equals(
                            question.getCorrect()
                    )
            )
            .count();
}

public int getIncorrectAnswers() {
    return getAnsweredQuestions()
            - getCorrectAnswers();
}

    public double getAverageScore() {

        double average = questions.stream()
                .filter(ActiveQuestion::isAnswered)
                .filter(question ->
                        question.getScore() != null
                )
                .mapToInt(ActiveQuestion::getScore)
                .average()
                .orElse(0.0);

        return Math.round(average * 100.0) / 100.0;
    }
}