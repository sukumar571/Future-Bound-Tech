package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** A mock test presented to a student for taking: metadata + answer-free questions. */
@Data
@Accessors(chain = true)
public class MockTestTakeDto {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer passingMarks;
    private int totalMarks;
    private List<PracticeQuestionTakeDto> questions = new ArrayList<>();
}
