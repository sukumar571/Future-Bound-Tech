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

/** Records a student's single free-practice MCQ attempt, used to track performance. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "practice_attempts")
public class PracticeAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private PracticeQuestion question;

    /** Option key the student selected: A / B / C / D. */
    @Size(max = 1)
    private String selectedOption;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;
}
