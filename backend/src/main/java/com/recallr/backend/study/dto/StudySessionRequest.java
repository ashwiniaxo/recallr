package com.recallr.backend.study.dto;

import java.util.List;

import com.recallr.backend.study.model.QuestionType;

public record StudySessionRequest(
        int questionCount,
        List<QuestionType> questionTypes
) {
}
