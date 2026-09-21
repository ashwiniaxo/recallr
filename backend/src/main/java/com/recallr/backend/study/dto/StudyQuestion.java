package com.recallr.backend.study.dto;

import java.util.List;

import com.recallr.backend.study.model.QuestionType;

public record StudyQuestion(
        int questionNumber,
        Long materialId,
        QuestionType type,
        String question,
        List<String> options
) {
}
