package com.recallr.backend.study.dto;

import java.util.List;

public record StudySessionResponse(
        Long sectionId,
        int questionCount,
        List<StudyQuestion> questions
) {
}