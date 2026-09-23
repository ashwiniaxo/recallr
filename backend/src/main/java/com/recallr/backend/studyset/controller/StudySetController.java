package com.recallr.backend.studyset.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.service.StudySetService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/study-sets")
public class StudySetController {

    private final StudySetService studySetService;

    public StudySetController(
            StudySetService studySetService
    ) {
        this.studySetService = studySetService;
    }

    @GetMapping
    public List<StudySet> getAllStudySets() {
        return studySetService.getAllStudySets();
    }

    @PostMapping
    public StudySet createStudySet(
            @RequestBody StudySet studySet
    ) {
        return studySetService.createStudySet(
                studySet
        );
    }

    @PutMapping("/{id}")
    public StudySet updateStudySet(
            @PathVariable Long id,
            @RequestBody StudySet studySet
    ) {
        return studySetService.updateStudySet(
                id,
                studySet
        );
    }

    @DeleteMapping("/{id}")
    public void deleteStudySet(
            @PathVariable Long id
    ) {
        studySetService.deleteStudySet(id);
    }
}