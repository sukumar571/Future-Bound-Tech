package com.futureboundtech.entity;

import com.futureboundtech.enums.QuestionCategory;
import com.futureboundtech.enums.QuestionDifficulty;
import com.futureboundtech.enums.QuestionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single item in the practice question bank. MCQ items carry options, a correct
 * answer, an explanation and marks; CODING items carry a problem statement, I/O
 * format, constraints, examples and tags and are studied only (never executed).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "practice_questions")
public class PracticeQuestion extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private QuestionType type = QuestionType.MCQ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionCategory category;

    @Size(max = 120)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QuestionDifficulty difficulty = QuestionDifficulty.EASY;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // ----- MCQ-only fields -----
    @Size(max = 500) private String optionA;
    @Size(max = 500) private String optionB;
    @Size(max = 500) private String optionC;
    @Size(max = 500) private String optionD;

    /** The chosen correct option key: A / B / C / D. Only set for MCQ items. */
    @Size(max = 1)
    private String correctOption;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Min(0)
    @Builder.Default
    private Integer marks = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;

    // ----- CODING-only fields -----
    @Column(columnDefinition = "TEXT") private String inputFormat;
    @Column(columnDefinition = "TEXT") private String outputFormat;
    @Column(columnDefinition = "TEXT") private String constraintsText;
    @Column(columnDefinition = "TEXT") private String examples;

    @Size(max = 500)
    private String tags;

    public boolean isCoding() {
        return type == QuestionType.CODING;
    }
}
