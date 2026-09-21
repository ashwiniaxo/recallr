package com.recallr.backend.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recallr.backend.study.model.StudySession;

public interface StudySessionRepository
        extends JpaRepository<StudySession, Long> {
}