package com.futureboundtech.enums;

/**
 * Publication state of a quiz. Students can only view and attempt {@link #PUBLISHED}
 * quizzes; a {@link #DRAFT} quiz is being prepared by the trainer.
 */
public enum QuizStatus {
    DRAFT,
    PUBLISHED
}
