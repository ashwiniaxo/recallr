package com.recallr.backend.coursesection.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.recallr.backend.coursesection.model.CourseSection;
import com.recallr.backend.coursesection.repository.CourseSectionRepository;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;
import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.repository.StudySetRepository;

@Service
public class CourseSectionService {

    private final CourseSectionRepository courseSectionRepository;
    private final StudySetRepository studySetRepository;
    private final StudyMaterialRepository studyMaterialRepository;

    public CourseSectionService(
            CourseSectionRepository courseSectionRepository,
            StudySetRepository studySetRepository,
            StudyMaterialRepository studyMaterialRepository
    ) {
        this.courseSectionRepository =
                courseSectionRepository;

        this.studySetRepository =
                studySetRepository;

        this.studyMaterialRepository =
                studyMaterialRepository;
    }

    public List<CourseSection> getSectionsByStudySet(
            Long studySetId
    ) {
        return courseSectionRepository
                .findByStudySetIdOrderBySectionNumberAsc(
                        studySetId
                );
    }

    public CourseSection createSection(
            Long studySetId,
            CourseSection courseSection
    ) {

        StudySet studySet =
                studySetRepository
                        .findById(studySetId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study set not found"
                                )
                        );

        courseSection.setStudySet(studySet);

        return courseSectionRepository.save(
                courseSection
        );
    }

    public CourseSection updateSection(
            Long studySetId,
            Long sectionId,
            CourseSection updatedSection
    ) {

        CourseSection section =
                courseSectionRepository
                        .findById(sectionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course section not found"
                                )
                        );

        if (!section
                .getStudySet()
                .getId()
                .equals(studySetId)) {

            throw new IllegalArgumentException(
                    "Course section does not belong to this study set"
            );
        }

        section.setSectionNumber(
                updatedSection.getSectionNumber()
        );

        section.setTitle(
                updatedSection.getTitle()
        );

        return courseSectionRepository.save(
                section
        );
    }

    public void deleteSection(
            Long studySetId,
            Long sectionId
    ) {

        CourseSection section =
                courseSectionRepository
                        .findById(sectionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course section not found"
                                )
                        );

        if (!section
                .getStudySet()
                .getId()
                .equals(studySetId)) {

            throw new IllegalArgumentException(
                    "Course section does not belong to this study set"
            );
        }

        boolean hasMaterials =
                !studyMaterialRepository
                        .findByCourseSectionId(sectionId)
                        .isEmpty();

        if (hasMaterials) {
            throw new IllegalStateException(
                    "Cannot delete a section that still contains study materials"
            );
        }

        courseSectionRepository.delete(section);
    }
}