package com.recallr.backend.study.session;

import java.util.List;

import com.recallr.backend.study.model.QuestionType;

public class ActiveQuestion {

    private final String questionId;
    private final Long materialId;
    private final QuestionType type;
    private final String question;
    private final List<String> options;
    private final Integer correctOptionIndex;
    private final String correctAnswer;
    private final String explanation;

    private boolean answered = false;
    private Integer score;
    private Boolean correct;

    public ActiveQuestion(
            String questionId,
            Long materialId,
            QuestionType type,
            String question,
            List<String> options,
            Integer correctOptionIndex,
            String correctAnswer,
            String explanation
    ) {
        this.questionId = questionId;
        this.materialId = materialId;
        this.type = type;
        this.question = question;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    public String questionId() {
        return questionId;
    }

    public Long materialId() {
        return materialId;
    }

    public QuestionType type() {
        return type;
    }

    public String question() {
        return question;
    }

    public List<String> options() {
        return options;
    }

    public Integer correctOptionIndex() {
        return correctOptionIndex;
    }

    public String correctAnswer() {
        return correctAnswer;
    }

    public String explanation() {
        return explanation;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void markAnswered(
            int score,
            boolean correct
    ) {
        this.answered = true;
        this.score = score;
        this.correct = correct;
    }
    public Integer getScore() {
        return score;
    }

    public Boolean getCorrect() {
        return correct;
    }
}