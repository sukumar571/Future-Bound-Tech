package com.futureboundtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single question's outcome within a mock test attempt, enabling per-question review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mock_test_answers")
public class MockTestAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private MockTestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private PracticeQuestion question;

    /** Option key the student selected: A / B / C / D (nullable if skipped). */
    @Size(max = 1)
    private String selectedOption;

    @Column(nullable = false)
    private boolean correct;

    /** Marks awarded for this question (full marks if correct, else 0). */
    @Column(name = "marks_awarded")
    private Integer marksAwarded;
}
