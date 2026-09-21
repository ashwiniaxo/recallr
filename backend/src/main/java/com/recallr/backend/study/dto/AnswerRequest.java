package com.recallr.backend.study.dto;

public record AnswerRequest(
        Integer selectedOptionIndex,
        String answer
) {
}