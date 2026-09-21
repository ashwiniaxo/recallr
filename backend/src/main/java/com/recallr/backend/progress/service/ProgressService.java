package com.recallr.backend.progress.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.recallr.backend.progress.model.ConceptProgress;
import com.recallr.backend.progress.repository.ConceptProgressRepository;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class ProgressService {

    private final ConceptProgressRepository progressRepository;
    private final StudyMaterialRepository studyMaterialRepository;

    public ProgressService(
            ConceptProgressRepository progressRepository,
            StudyMaterialRepository studyMaterialRepository
    ) {
        this.progressRepository = progressRepository;
        this.studyMaterialRepository = studyMaterialRepository;
    }

    public void recordAnswer(
            Long materialId,
            int score,
            boolean correct
    ) {

        StudyMaterial material =
                studyMaterialRepository.findById(materialId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Study material not found"
                                )
                        );

        ConceptProgress progress =
                progressRepository
                        .findByStudyMaterialId(materialId)
                        .orElseGet(() ->
                                new ConceptProgress(material)
                        );

        int previousAttempts =
                progress.getAttempts();

        double previousAverage =
                progress.getAverageScore();

        double newAverage =
                (
                    previousAverage * previousAttempts
                    + score
                ) / (previousAttempts + 1);

        progress.setAttempts(
                previousAttempts + 1
        );

        if (correct) {
            progress.setCorrectAnswers(
                    progress.getCorrectAnswers() + 1
            );
        }

        progress.setAverageScore(newAverage);

        // Version 1:
        // mastery = moyenne des résultats.
        progress.setMasteryScore(newAverage);

        progress.setLastReviewedAt(
                LocalDateTime.now()
        );

        progressRepository.save(progress);
    }
}