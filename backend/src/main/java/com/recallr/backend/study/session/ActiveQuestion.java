package com.recallr.backend.study.session;

import java.util.List;

import com.recallr.backend.study.model.QuestionType;

public record ActiveQuestion(
        String questionId,
        Long materialId,
        QuestionType type,
        String question,
        List<String> options,
        Integer correctOptionIndex,
        String correctAnswer,
        String explanation
) {
}