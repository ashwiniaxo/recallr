package com.recallr.backend.study.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class StudySessionStore {

    private final Map<String, ActiveStudySession> sessions =
            new ConcurrentHashMap<>();

    public void save(ActiveStudySession session) {
        sessions.put(session.getSessionId(), session);
    }

    public Optional<ActiveStudySession> findById(String sessionId) {
        return Optional.ofNullable(
                sessions.get(sessionId)
        );
    }

    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}