package com.recallr.backend.studyset.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.recallr.backend.coursesection.repository.CourseSectionRepository;
import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.repository.StudySetRepository;

@Service
public class StudySetService {

    private final StudySetRepository studySetRepository;
    private final CourseSectionRepository courseSectionRepository;

    public StudySetService(
            StudySetRepository studySetRepository,
            CourseSectionRepository courseSectionRepository
    ) {
        this.studySetRepository = studySetRepository;
        this.courseSectionRepository = courseSectionRepository;
    }

    public List<StudySet> getAllStudySets() {
        return studySetRepository.findAll();
    }

    public StudySet createStudySet(
            StudySet studySet
    ) {
        return studySetRepository.save(
                studySet
        );
    }

    public StudySet updateStudySet(
            Long id,
            StudySet updatedStudySet
    ) {
        StudySet studySet =
                studySetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study set not found"
                                )
                        );

        studySet.setTitle(
                updatedStudySet.getTitle()
        );

        studySet.setDescription(
                updatedStudySet.getDescription()
        );

        return studySetRepository.save(
                studySet
        );
    }

    public void deleteStudySet(Long id) {

        StudySet studySet =
                studySetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study set not found"
                                )
                        );

        boolean hasSections =
                !courseSectionRepository
                        .findByStudySetIdOrderBySectionNumberAsc(id)
                        .isEmpty();

        if (hasSections) {
            throw new IllegalStateException(
                    "Cannot delete a study set that still contains sections"
            );
        }

        studySetRepository.delete(studySet);
    }
}