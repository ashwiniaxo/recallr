package com.recallr.backend.studymaterial.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.recallr.backend.coursesection.model.CourseSection;
import com.recallr.backend.coursesection.repository.CourseSectionRepository;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final CourseSectionRepository courseSectionRepository;

    public StudyMaterialService(
            StudyMaterialRepository studyMaterialRepository,
            CourseSectionRepository courseSectionRepository
    ) {
        this.studyMaterialRepository = studyMaterialRepository;
        this.courseSectionRepository = courseSectionRepository;
    }

    public List<StudyMaterial> getMaterialsBySection(Long sectionId) {
        return studyMaterialRepository.findByCourseSectionId(sectionId);
    }

    public StudyMaterial createMaterial(
            Long sectionId,
            StudyMaterial studyMaterial
    ) {
        CourseSection courseSection = courseSectionRepository
                .findById(sectionId)
                .orElseThrow(() ->
                        new RuntimeException("Course section not found")
                );

        studyMaterial.setCourseSection(courseSection);

        return studyMaterialRepository.save(studyMaterial);
    }

    public StudyMaterial updateMaterial(
            Long sectionId,
            Long materialId,
            StudyMaterial updatedMaterial
    ) {
        StudyMaterial existingMaterial = studyMaterialRepository
                .findById(materialId)
                .orElseThrow(() ->
                        new RuntimeException("Study material not found")
                );

        if (!existingMaterial
                .getCourseSection()
                .getId()
                .equals(sectionId)) {
            throw new RuntimeException(
                    "Study material does not belong to this section"
            );
        }

        existingMaterial.setConcept(
                updatedMaterial.getConcept()
        );

        existingMaterial.setContent(
                updatedMaterial.getContent()
        );

        return studyMaterialRepository.save(
                existingMaterial
        );
    }

    public void deleteMaterial(
            Long sectionId,
            Long materialId
    ) {
        StudyMaterial existingMaterial = studyMaterialRepository
                .findById(materialId)
                .orElseThrow(() ->
                        new RuntimeException("Study material not found")
                );

        if (!existingMaterial
                .getCourseSection()
                .getId()
                .equals(sectionId)) {
            throw new RuntimeException(
                    "Study material does not belong to this section"
            );
        }

        studyMaterialRepository.delete(existingMaterial);
    }
}