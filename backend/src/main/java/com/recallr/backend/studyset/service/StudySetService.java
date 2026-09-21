package com.recallr.backend.studyset.service;

import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.repository.StudySetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudySetService {

    private final StudySetRepository studySetRepository;

    public StudySetService(StudySetRepository studySetRepository) {
        this.studySetRepository = studySetRepository;
    }

    public List<StudySet> getAllStudySets() {
        return studySetRepository.findAll();
    }

    public StudySet createStudySet(StudySet studySet) {
        return studySetRepository.save(studySet);
    }

    public StudySet updateStudySet(Long id, StudySet updatedStudySet) {
        StudySet studySet = studySetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study set not found"));

        studySet.setTitle(updatedStudySet.getTitle());
        studySet.setDescription(updatedStudySet.getDescription());

        return studySetRepository.save(studySet);
    }
}
