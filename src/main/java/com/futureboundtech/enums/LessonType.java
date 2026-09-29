package com.futureboundtech.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LessonType {
    VIDEO("Video", "bi-play-btn-fill", "text-bg-danger"),
    READING("Reading", "bi-journal-text", "text-bg-info"),
    CODING("Coding", "bi-code-slash", "text-bg-dark"),
    ASSIGNMENT("Assignment", "bi-clipboard-check", "text-bg-warning"),
    QUIZ("Quiz", "bi-patch-question", "text-bg-primary"),
    PROJECT("Project", "bi-kanban", "text-bg-success");

    private final String label;
    private final String icon;
    private final String badgeCss;
}
