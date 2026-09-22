package com.recallr.backend.study.dto;

public record StudySessionStatusResponse(
        String sessionId,
        Long sectionId,
        int totalQuestions,
        int answeredQuestions,
        int remainingQuestions,
        int correctAnswers,
        int incorrectAnswers,
        double averageScore,
        boolean completed
) {
}