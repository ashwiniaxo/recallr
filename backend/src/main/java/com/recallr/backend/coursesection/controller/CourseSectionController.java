package com.recallr.backend.coursesection.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recallr.backend.coursesection.model.CourseSection;
import com.recallr.backend.coursesection.service.CourseSectionService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/study-sets/{studySetId}/sections")
public class CourseSectionController {

    private final CourseSectionService courseSectionService;

    public CourseSectionController(
            CourseSectionService courseSectionService
    ) {
        this.courseSectionService = courseSectionService;
    }

    @GetMapping
    public List<CourseSection> getSections(
            @PathVariable("studySetId") Long studySetId
    ) {
        return courseSectionService.getSectionsByStudySet(studySetId);
    }

    @PostMapping
    public CourseSection createSection(
            @PathVariable("studySetId") Long studySetId,
            @RequestBody CourseSection courseSection
    ) {
        return courseSectionService.createSection(
                studySetId,
                courseSection
        );
    }
}
