package com.recallr.backend.progress.dto;

import java.time.LocalDateTime;

public record ConceptProgressResponse(
        Long materialId,
        String concept,
        int attempts,
        int correctAnswers,
        double averageScore,
        double masteryScore,
        LocalDateTime lastReviewedAt
) {
}