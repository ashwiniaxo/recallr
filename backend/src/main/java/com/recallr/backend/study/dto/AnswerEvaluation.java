package com.recallr.backend.study.dto;

import java.util.List;

public record AnswerEvaluation(
        int score,
        boolean correct,
        String feedback,
        String explanation,
        String expectedAnswer,
        List<String> correctConcepts,
        List<String> missingConcepts
) {
}