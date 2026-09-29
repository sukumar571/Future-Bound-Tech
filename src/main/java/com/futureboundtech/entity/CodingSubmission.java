package com.futureboundtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A student's own attempt at a study-only coding problem. The code is stored for the
 * student to keep working on it — it is NEVER executed or judged by this application
 * (the coding sandbox stays external by design). One row per (student, question).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coding_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "question_id"}))
public class CodingSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private PracticeQuestion question;

    @Size(max = 20000)
    @Column(columnDefinition = "TEXT")
    private String code;

    @Size(max = 40)
    private String language;

    @Column(nullable = false)
    @Builder.Default
    private boolean solved = false;

    private LocalDateTime lastSavedAt;
}
