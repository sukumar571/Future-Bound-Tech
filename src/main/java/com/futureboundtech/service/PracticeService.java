package com.futureboundtech.service;

import com.futureboundtech.dto.*;
import com.futureboundtech.entity.User;

import java.util.List;
import java.util.Map;

/**
 * Backs the Phase 16 practice platform: an admin-managed question bank (MCQ + coding
 * library), timed mock tests, and student attempt/performance tracking. All student
 * operations resolve the current user and enforce published-only visibility server-side.
 */
public interface PracticeService {

    // ===================== Admin: questions =====================
    List<PracticeQuestionDto> listQuestions(String type, String category, String difficulty,
                                            String topic, String query);

    PracticeQuestionDto getQuestion(Long id);

    void createQuestion(PracticeQuestionFormDto form);

    void updateQuestion(Long id, PracticeQuestionFormDto form);

    void deleteQuestion(Long id);

    void setQuestionPublished(Long id, boolean published);

    /** Bulk-import questions from JSON-lines or CSV text. Returns the number imported. */
    int importQuestions(String payload, String format);

    List<String> listTopics();

    // ===================== Admin: mock tests =====================
    List<MockTestDto> listTests();

    MockTestFormDto getTestForm(Long id);

    /** All published MCQ questions, for the test-builder picker. */
    List<PracticeQuestionDto> listMcqQuestionsForPicker();

    void createTest(MockTestFormDto form);

    void updateTest(Long id, MockTestFormDto form);

    void deleteTest(Long id);

    void setTestPublished(Long id, boolean published);

    // ===================== Student =====================
    /** Category cards with published MCQ counts for the practice hub. */
    List<NameValueDto> mcqCategoryCounts();

    /** Public marketing stats for the /practice landing page (no personal data). */
    Map<String, Long> publicPracticeStats();

    List<PracticeQuestionTakeDto> browseMcq(String category, String difficulty, String topic);

    /**
     * Ordered ids of the published MCQs matching the same filters as {@link #browseMcq},
     * used by the sequential practice runner (Back/Next navigation with a position counter).
     */
    List<Long> mcqSequenceIds(String category, String difficulty, String topic);

    PracticeQuestionTakeDto getQuestionToAttempt(Long id);

    PracticeQuestionResultDto attemptQuestion(User user, Long questionId, String selectedOption);

    /** Published coding problems (study-only library). */
    List<PracticeQuestionDto> browseCoding(String category, String difficulty, String topic);

    PracticeQuestionDto getCodingProblem(Long id);

    /** The student's saved working solution for a coding problem (empty DTO if none yet). */
    CodingSolutionDto getSolution(User user, Long questionId);

    /** Saves (upserts) the student's own solution draft and/or solved flag. Never executed. */
    void saveSolution(User user, Long questionId, String code, String language, boolean solved);

    List<MockTestDto> listAvailableTests(User user);

    MockTestTakeDto getTestToTake(User user, Long testId);

    MockTestResultDto submitTest(User user, Long testId, Map<Long, String> answers);

    MockTestResultDto getTestResult(User user, Long attemptId);

    PracticePerformanceDto performance(User user);
}
