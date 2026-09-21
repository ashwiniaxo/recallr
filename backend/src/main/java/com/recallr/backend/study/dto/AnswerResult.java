package com.recallr.backend.study.dto;

import java.util.List;

public record AnswerResult(
        boolean correct,
        Integer score,
        String feedback,
        String explanation,
        String expectedAnswer,
        List<String> correctConcepts,
        List<String> missingConcepts
) {
}