package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Auto-scored result of a mock test attempt, with per-question review. */
@Data
@Accessors(chain = true)
public class MockTestResultDto {
    private Long attemptId;
    private String title;
    private Integer scoreObtained;
    private Integer totalMarks;
    private Integer percentage;
    private Integer correctCount;
    private Integer totalQuestions;
    private boolean passed;
    private LocalDateTime submittedAt;
    private List<ReviewRow> review = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class ReviewRow {
        private String questionText;
        private String selectedOption;
        private String correctOption;
        private boolean correct;
        private Integer marksAwarded;
        private String explanation;
    }
}
