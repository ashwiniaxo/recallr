package com.recallr.backend.studyset.controller;

import com.recallr.backend.studyset.model.StudySet;
import com.recallr.backend.studyset.service.StudySetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/study-sets")
public class StudySetController {

    private final StudySetService studySetService;

    public StudySetController(StudySetService studySetService) {
        this.studySetService = studySetService;
    }

    @GetMapping
    public List<StudySet> getAllStudySets() {
        return studySetService.getAllStudySets();
    }

    @PostMapping
    public StudySet createStudySet(@RequestBody StudySet studySet) {
        return studySetService.createStudySet(studySet);
    }

    @PutMapping("/{id}")
    public StudySet updateStudySet(
            @PathVariable Long id,
            @RequestBody StudySet studySet
    ) {
        return studySetService.updateStudySet(id, studySet);
    }
}