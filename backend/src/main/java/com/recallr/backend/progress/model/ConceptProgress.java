package com.recallr.backend.progress.model;

import java.time.LocalDateTime;

import com.recallr.backend.studymaterial.model.StudyMaterial;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "concept_progress")
public class ConceptProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "study_material_id",
            nullable = false,
            unique = true
    )
    private StudyMaterial studyMaterial;

    private int attempts = 0;

    private int correctAnswers = 0;

    private double averageScore = 0.0;

    private double masteryScore = 0.0;

    private LocalDateTime lastReviewedAt;

    public ConceptProgress() {
    }

    public ConceptProgress(StudyMaterial studyMaterial) {
        this.studyMaterial = studyMaterial;
    }

    public Long getId() {
        return id;
    }

    public StudyMaterial getStudyMaterial() {
        return studyMaterial;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public double getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(double masteryScore) {
        this.masteryScore = masteryScore;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}