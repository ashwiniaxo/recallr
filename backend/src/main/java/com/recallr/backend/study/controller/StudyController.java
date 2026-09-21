package com.recallr.backend.study.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.service.QuestionGeneratorService;

@RestController
@RequestMapping("/api/study")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyController {

    private final QuestionGeneratorService questionGeneratorService;

    public StudyController(
            QuestionGeneratorService questionGeneratorService
    ) {
        this.questionGeneratorService = questionGeneratorService;
    }

    @PostMapping("/materials/{materialId}/questions")
    public GeneratedQuestion generateQuestion(
            @PathVariable("materialId") Long materialId,
            @RequestParam("type") QuestionType type
    ) {
        return questionGeneratorService.generateQuestion(
                materialId,
                type
        );
    }
}