package com.recallr.backend.studyset.repository;

import com.recallr.backend.studyset.model.StudySet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySetRepository extends JpaRepository<StudySet, Long> {
}
