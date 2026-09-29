package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class StudentQuizTakeDto {

    private Long quizId;
    private String title;
    private String courseTitle;
    private int timeLimitMinutes;
    private int passingScore;
    private List<Question> questions = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class Question {
        private Long id;
        private String questionText;
        private List<Option> options = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    @lombok.AllArgsConstructor
    public static class Option {
        private String key;   // A/B/C/D
        private String text;
    }
}
