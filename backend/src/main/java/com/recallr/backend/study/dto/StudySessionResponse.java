package com.recallr.backend.study.dto;

import java.util.List;

public record StudySessionResponse(
        String sessionId,
        Long sectionId,
        int questionCount,
        List<StudyQuestion> questions
) {
}