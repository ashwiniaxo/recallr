package com.recallr.backend.coursesection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recallr.backend.coursesection.model.CourseSection;

public interface CourseSectionRepository
        extends JpaRepository<CourseSection, Long> {

    List<CourseSection> findByStudySetIdOrderBySectionNumberAsc(Long studySetId);
}