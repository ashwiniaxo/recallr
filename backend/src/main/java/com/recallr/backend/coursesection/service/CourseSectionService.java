package com.recallr.backend.coursesection.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.recallr.backend.coursesection.model.CourseSection;
import com.recallr.backend.coursesection.repository.CourseSectionRepository;
import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.repository.StudySetRepository;

@Service
public class CourseSectionService {

    private final CourseSectionRepository courseSectionRepository;
    private final StudySetRepository studySetRepository;

    public CourseSectionService(
            CourseSectionRepository courseSectionRepository,
            StudySetRepository studySetRepository
    ) {
        this.courseSectionRepository = courseSectionRepository;
        this.studySetRepository = studySetRepository;
    }

    public List<CourseSection> getSectionsByStudySet(Long studySetId) {
        return courseSectionRepository
                .findByStudySetIdOrderBySectionNumberAsc(studySetId);
    }

    public CourseSection createSection(
            Long studySetId,
            CourseSection courseSection
    ) {
        StudySet studySet = studySetRepository.findById(studySetId)
                .orElseThrow(() -> new RuntimeException("Study set not found"));

        courseSection.setStudySet(studySet);

        return courseSectionRepository.save(courseSection);
    }
}