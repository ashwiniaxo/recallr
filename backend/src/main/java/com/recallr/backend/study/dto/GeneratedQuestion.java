package com.recallr.backend.study.dto;

import java.util.List;

import com.recallr.backend.study.model.QuestionType;

public record GeneratedQuestion(
        QuestionType type,
        String question,
        List<String> options,
        String correctAnswer,
        String explanation
) {
}