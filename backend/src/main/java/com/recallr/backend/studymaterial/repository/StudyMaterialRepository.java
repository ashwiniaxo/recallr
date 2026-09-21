package com.recallr.backend.studymaterial.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recallr.backend.studymaterial.model.StudyMaterial;

public interface StudyMaterialRepository
        extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByCourseSectionId(Long courseSectionId);
}
