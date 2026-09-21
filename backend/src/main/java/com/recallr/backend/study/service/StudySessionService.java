package com.recallr.backend.study.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.dto.StudyQuestion;
import com.recallr.backend.study.dto.StudySessionRequest;
import com.recallr.backend.study.dto.StudySessionResponse;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.repository.StudyMaterialRepository;

@Service
public class StudySessionService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final QuestionGeneratorService questionGeneratorService;

    public StudySessionService(
            StudyMaterialRepository studyMaterialRepository,
            QuestionGeneratorService questionGeneratorService
    ) {
        this.studyMaterialRepository = studyMaterialRepository;
        this.questionGeneratorService = questionGeneratorService;
    }

    public StudySessionResponse createSession(
            Long sectionId,
            StudySessionRequest request
    ) {

        List<StudyMaterial> materials =
                studyMaterialRepository.findByCourseSectionId(sectionId);

        if (materials.isEmpty()) {
            throw new RuntimeException(
                    "No study materials found for this section"
            );
        }

        if (request.questionCount() <= 0) {
            throw new IllegalArgumentException(
                    "Question count must be greater than 0"
            );
        }

        if (request.questionTypes() == null ||
                request.questionTypes().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one question type is required"
            );
        }

        List<StudyMaterial> shuffledMaterials =
                new ArrayList<>(materials);

        Collections.shuffle(shuffledMaterials);

        List<StudyQuestion> questions = new ArrayList<>();

        for (int i = 0; i < request.questionCount(); i++) {

            StudyMaterial material =
                    shuffledMaterials.get(
                            i % shuffledMaterials.size()
                    );

            QuestionType type =
                    request.questionTypes().get(
                            ThreadLocalRandom.current().nextInt(
                                    request.questionTypes().size()
                            )
                    );

            GeneratedQuestion generated =
                    questionGeneratorService.generateQuestion(
                            material.getId(),
                            type
                    );

            StudyQuestion question = new StudyQuestion(
                    i + 1,
                    material.getId(),
                    generated.type(),
                    generated.question(),
                    generated.options()
            );

            questions.add(question);
        }

        return new StudySessionResponse(
                sectionId,
                questions.size(),
                questions
        );
    }
}