package com.futureboundtech.entity;

import com.futureboundtech.enums.QuizStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.util.List;
import java.util.ArrayList;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quizs")
public class Quiz extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @Min(1)
    private Integer timeLimitMinutes;
    
    @Min(0)
    private Integer passingScore;

    /** Maximum attempts a student may take. Null = unlimited. */
    @Min(1)
    @Column(name = "attempt_limit")
    private Integer attemptLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuizStatus status = QuizStatus.PUBLISHED;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuizQuestion> questions = new ArrayList<>();

    public boolean isPublished() {
        return status == QuizStatus.PUBLISHED;
    }
}
