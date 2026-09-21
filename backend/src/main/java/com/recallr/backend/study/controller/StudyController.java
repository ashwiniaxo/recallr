package com.recallr.backend.study.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.study.dto.AnswerRequest;
import com.recallr.backend.study.dto.AnswerResult;
import com.recallr.backend.study.dto.GeneratedQuestion;
import com.recallr.backend.study.dto.StudySessionRequest;
import com.recallr.backend.study.dto.StudySessionResponse;
import com.recallr.backend.study.model.QuestionType;
import com.recallr.backend.study.service.QuestionGeneratorService;
import com.recallr.backend.study.service.StudySessionService;

@RestController
@RequestMapping("/api/study")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyController {

    private final QuestionGeneratorService questionGeneratorService;
    private final StudySessionService studySessionService;

    public StudyController(
            QuestionGeneratorService questionGeneratorService,
            StudySessionService studySessionService
    ) {
        this.questionGeneratorService = questionGeneratorService;
        this.studySessionService = studySessionService;
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

    @PostMapping("/sections/{sectionId}/sessions")
    public StudySessionResponse createSession(
            @PathVariable("sectionId") Long sectionId,
            @RequestBody StudySessionRequest request
    ) {
        return studySessionService.createSession(
                sectionId,
                request
        );
    }

    @PostMapping(
        "/sessions/{sessionId}/questions/{questionId}/answer"
        )
        public AnswerResult answerQuestion(
                @PathVariable String sessionId,
                @PathVariable String questionId,
                @RequestBody AnswerRequest request
        ) {
        return studySessionService.answerQuestion(
                sessionId,
                questionId,
                request
        );
        }
}