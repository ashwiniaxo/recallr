package com.recallr.backend.study.session;

import java.time.LocalDateTime;
import java.util.List;

public class ActiveStudySession {

    private final String sessionId;
    private final Long sectionId;
    private final LocalDateTime startedAt;
    private final List<ActiveQuestion> questions;

    public ActiveStudySession(
            String sessionId,
            Long sectionId,
            List<ActiveQuestion> questions
    ) {
        this.sessionId = sessionId;
        this.sectionId = sectionId;
        this.questions = questions;
        this.startedAt = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public List<ActiveQuestion> getQuestions() {
        return questions;
    }

    public ActiveQuestion findQuestion(String questionId) {
        return questions.stream()
                .filter(question ->
                        question.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Question not found in this session"
                        )
                );
    }
}