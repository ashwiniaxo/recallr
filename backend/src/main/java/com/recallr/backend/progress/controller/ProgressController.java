package com.recallr.backend.progress.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.progress.dto.SectionProgressResponse;
import com.recallr.backend.progress.service.ProgressService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(
            ProgressService progressService
    ) {
        this.progressService = progressService;
    }

    @GetMapping("/sections/{sectionId}")
    public SectionProgressResponse getSectionProgress(
            @PathVariable Long sectionId
    ) {
        return progressService.getSectionProgress(
                sectionId
        );
    }
}