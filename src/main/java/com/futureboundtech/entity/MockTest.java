package com.futureboundtech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A timed mock test assembled from MCQ practice questions. Students take it under
 * {@code durationMinutes}, it is auto-scored against {@code passingMarks}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mock_tests")
public class MockTest extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Min(0)
    @Column(name = "passing_marks")
    private Integer passingMarks;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mock_test_questions",
            joinColumns = @JoinColumn(name = "mock_test_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id"))
    @Builder.Default
    private List<PracticeQuestion> questions = new ArrayList<>();
}
