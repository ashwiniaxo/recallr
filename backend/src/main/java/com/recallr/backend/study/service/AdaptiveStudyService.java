package com.recallr.backend.study.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.recallr.backend.progress.model.ConceptProgress;
import com.recallr.backend.progress.repository.ConceptProgressRepository;
import com.recallr.backend.studymaterial.model.StudyMaterial;

@Service
public class AdaptiveStudyService {

    private final ConceptProgressRepository progressRepository;

    public AdaptiveStudyService(
            ConceptProgressRepository progressRepository
    ) {
        this.progressRepository = progressRepository;
    }

    public List<StudyMaterial> prioritizeMaterials(
            List<StudyMaterial> materials
    ) {

        List<StudyMaterial> prioritized =
                new ArrayList<>(materials);

        prioritized.sort(
                Comparator.comparingDouble(
                        this::getPriority
                ).reversed()
        );

        return prioritized;
    }

    private double getPriority(
            StudyMaterial material
    ) {

        ConceptProgress progress =
                progressRepository
                        .findByStudyMaterialId(material.getId())
                        .orElse(null);

        if (progress == null ||
                progress.getAttempts() == 0) {
            return 100.0;
        }

        return 100.0 - progress.getMasteryScore();
    }
}