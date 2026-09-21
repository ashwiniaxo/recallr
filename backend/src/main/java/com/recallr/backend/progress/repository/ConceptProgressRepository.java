package com.recallr.backend.progress.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recallr.backend.progress.model.ConceptProgress;

public interface ConceptProgressRepository
        extends JpaRepository<ConceptProgress, Long> {

    Optional<ConceptProgress> findByStudyMaterialId(
            Long studyMaterialId
    );
}