package com.recallr.backend.studymaterial.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.studymaterial.model.StudyMaterial;
import com.recallr.backend.studymaterial.service.StudyMaterialService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/sections/{sectionId}/materials")
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    public StudyMaterialController(
            StudyMaterialService studyMaterialService
    ) {
        this.studyMaterialService = studyMaterialService;
    }

    @GetMapping
    public List<StudyMaterial> getMaterials(
            @PathVariable("sectionId") Long sectionId
    ) {
        return studyMaterialService.getMaterialsBySection(sectionId);
    }

    @PostMapping
    public StudyMaterial createMaterial(
            @PathVariable("sectionId") Long sectionId,
            @RequestBody StudyMaterial studyMaterial
    ) {
        return studyMaterialService.createMaterial(
                sectionId,
                studyMaterial
        );
    }
}