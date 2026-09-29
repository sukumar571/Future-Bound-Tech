package com.futureboundtech.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futureboundtech.dto.*;
import com.futureboundtech.entity.*;
import com.futureboundtech.enums.*;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.*;
import com.futureboundtech.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    /** Tolerance granted on top of a test's duration before a submit is rejected. */
    private static final long TIME_GRACE_SECONDS = 60L;

    private final PracticeQuestionRepository questionRepository;
    private final MockTestRepository mockTestRepository;
    private final PracticeAttemptRepository practiceAttemptRepository;
    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final MockTestAnswerRepository mockTestAnswerRepository;
    private final StudentRepository studentRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =====================================================================
    // Admin — questions
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionDto> listQuestions(String type, String category, String difficulty,
                                                   String topic, String query) {
        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(q -> isBlank(type) || q.getType().name().equalsIgnoreCase(type.trim()))
                .filter(q -> isBlank(category) || q.getCategory().name().equalsIgnoreCase(category.trim()))
                .filter(q -> isBlank(difficulty) || q.getDifficulty().name().equalsIgnoreCase(difficulty.trim()))
                .filter(q -> isBlank(topic) || topic.trim().equalsIgnoreCase(nz(q.getTopic())))
                .filter(q -> isBlank(query) || nz(q.getQuestionText()).toLowerCase().contains(query.trim().toLowerCase()))
                .map(this::toQuestionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeQuestionDto getQuestion(Long id) {
        return toQuestionDto(requireQuestion(id));
    }

    @Override
    @Transactional
    public void createQuestion(PracticeQuestionFormDto form) {
        PracticeQuestion q = new PracticeQuestion();
        applyQuestionFields(q, form);
        questionRepository.save(q);
    }

    @Override
    @Transactional
    public void updateQuestion(Long id, PracticeQuestionFormDto form) {
        PracticeQuestion q = requireQuestion(id);
        applyQuestionFields(q, form);
        questionRepository.save(q);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        questionRepository.delete(requireQuestion(id));
    }

    @Override
    @Transactional
    public void setQuestionPublished(Long id, boolean published) {
        PracticeQuestion q = requireQuestion(id);
        q.setPublished(published);
        questionRepository.save(q);
    }

    @Override
    @Transactional
    public int importQuestions(String payload, String format) {
        if (isBlank(payload)) {
            throw new BusinessException("Paste some question data to import.");
        }
        List<PracticeQuestion> batch = new ArrayList<>();
        if ("csv".equalsIgnoreCase(nz(format))) {
            batch.addAll(parseCsvQuestions(payload));
        } else {
            batch.addAll(parseJsonQuestions(payload));
        }
        if (batch.isEmpty()) {
            throw new BusinessException("No valid questions were found in the input.");
        }
        questionRepository.saveAll(batch);
        return batch.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listTopics() {
        return questionRepository.findDistinctTopics();
    }

    // =====================================================================
    // Admin — mock tests
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<MockTestDto> listTests() {
        return mockTestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(t -> toTestDto(t, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MockTestFormDto getTestForm(Long id) {
        MockTest t = requireTest(id);
        MockTestFormDto form = new MockTestFormDto()
                .setId(t.getId())
                .setTitle(t.getTitle())
                .setDescription(t.getDescription())
                .setDurationMinutes(t.getDurationMinutes())
                .setPassingMarks(t.getPassingMarks())
                .setPublished(t.isPublished());
        form.setQuestionIds(t.getQuestions().stream().map(PracticeQuestion::getId).collect(Collectors.toList()));
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionDto> listMcqQuestionsForPicker() {
        return questionRepository.findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType.MCQ).stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createTest(MockTestFormDto form) {
        MockTest t = new MockTest();
        applyTestFields(t, form);
        mockTestRepository.save(t);
    }

    @Override
    @Transactional
    public void updateTest(Long id, MockTestFormDto form) {
        MockTest t = requireTest(id);
        applyTestFields(t, form);
        mockTestRepository.save(t);
    }

    @Override
    @Transactional
    public void deleteTest(Long id) {
        mockTestRepository.delete(requireTest(id));
    }

    @Override
    @Transactional
    public void setTestPublished(Long id, boolean published) {
        MockTest t = requireTest(id);
        t.setPublished(published);
        mockTestRepository.save(t);
    }

    // =====================================================================
    // Student — MCQ practice
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<NameValueDto> mcqCategoryCounts() {
        Map<QuestionCategory, Long> counts = questionRepository
                .findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType.MCQ).stream()
                .collect(Collectors.groupingBy(PracticeQuestion::getCategory, Collectors.counting()));
        List<NameValueDto> rows = new ArrayList<>();
        for (QuestionCategory c : QuestionCategory.values()) {
            rows.add(new NameValueDto(c.name(), counts.getOrDefault(c, 0L)));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> publicPracticeStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("mcqCount", questionRepository.countByTypeAndPublishedTrue(QuestionType.MCQ));
        stats.put("codingCount", questionRepository.countByTypeAndPublishedTrue(QuestionType.CODING));
        stats.put("mockTestCount", mockTestRepository.countByPublishedTrue());
        stats.put("topicCount", (long) questionRepository.findDistinctTopics().size());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionTakeDto> browseMcq(String category, String difficulty, String topic) {
        return filteredMcqs(category, difficulty, topic).stream()
                .map(this::toTakeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> mcqSequenceIds(String category, String difficulty, String topic) {
        return filteredMcqs(category, difficulty, topic).stream()
                .map(PracticeQuestion::getId)
                .collect(Collectors.toList());
    }

    /** Shared filter used by both the MCQ browse list and the sequential runner. */
    private List<PracticeQuestion> filteredMcqs(String category, String difficulty, String topic) {
        return questionRepository.findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType.MCQ).stream()
                .filter(q -> isBlank(category) || q.getCategory().name().equalsIgnoreCase(category.trim()))
                .filter(q -> isBlank(difficulty) || q.getDifficulty().name().equalsIgnoreCase(difficulty.trim()))
                .filter(q -> isBlank(topic) || topic.trim().equalsIgnoreCase(nz(q.getTopic())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeQuestionTakeDto getQuestionToAttempt(Long id) {
        PracticeQuestion q = requireQuestion(id);
        requirePublishedMcq(q);
        return toTakeDto(q);
    }

    @Override
    @Transactional
    public PracticeQuestionResultDto attemptQuestion(User user, Long questionId, String selectedOption) {
        Student student = requireStudent(user);
        PracticeQuestion q = requireQuestion(questionId);
        requirePublishedMcq(q);

        boolean correct = selectedOption != null && q.getCorrectOption() != null
                && selectedOption.trim().equalsIgnoreCase(q.getCorrectOption().trim());
        int awarded = correct ? safeMarks(q) : 0;

        PracticeAttempt attempt = PracticeAttempt.builder()
                .student(student)
                .question(q)
                .selectedOption(selectedOption == null ? null : selectedOption.trim().toUpperCase())
                .correct(correct)
                .attemptedAt(LocalDateTime.now())
                .build();
        practiceAttemptRepository.save(attempt);

        return new PracticeQuestionResultDto()
                .setQuestionId(q.getId())
                .setSelectedOption(selectedOption)
                .setCorrectOption(q.getCorrectOption())
                .setCorrect(correct)
                .setMarksAwarded(awarded)
                .setExplanation(q.getExplanation())
                .setQuestionText(q.getQuestionText());
    }

    // =====================================================================
    // Student — coding library (study-only, never executed)
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionDto> browseCoding(String category, String difficulty, String topic) {
        return questionRepository.findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType.CODING).stream()
                .filter(q -> isBlank(category) || q.getCategory().name().equalsIgnoreCase(category.trim()))
                .filter(q -> isBlank(difficulty) || q.getDifficulty().name().equalsIgnoreCase(difficulty.trim()))
                .filter(q -> isBlank(topic) || topic.trim().equalsIgnoreCase(nz(q.getTopic())))
                .map(this::toQuestionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeQuestionDto getCodingProblem(Long id) {
        PracticeQuestion q = requireQuestion(id);
        if (!q.isCoding() || !q.isPublished()) {
            throw new BusinessException("This coding problem is not available.");
        }
        return toQuestionDto(q);
    }

    @Override
    @Transactional(readOnly = true)
    public CodingSolutionDto getSolution(User user, Long questionId) {
        Student student = requireStudent(user);
        CodingSubmission sub = codingSubmissionRepository
                .findByStudent_IdAndQuestion_Id(student.getId(), questionId).orElse(null);
        if (sub == null) {
            return new CodingSolutionDto().setQuestionId(questionId);
        }
        return new CodingSolutionDto()
                .setQuestionId(questionId)
                .setCode(sub.getCode())
                .setLanguage(sub.getLanguage())
                .setSolved(sub.isSolved())
                .setLastSavedAt(sub.getLastSavedAt());
    }

    @Override
    @Transactional
    public void saveSolution(User user, Long questionId, String code, String language, boolean solved) {
        Student student = requireStudent(user);
        PracticeQuestion q = requireQuestion(questionId);
        if (!q.isCoding() || !q.isPublished()) {
            throw new BusinessException("This coding problem is not available.");
        }
        CodingSubmission sub = codingSubmissionRepository
                .findByStudent_IdAndQuestion_Id(student.getId(), questionId)
                .orElseGet(() -> CodingSubmission.builder().student(student).question(q).build());
        sub.setCode(trimToNull(code));
        sub.setLanguage(trimToNull(language));
        sub.setSolved(solved);
        sub.setLastSavedAt(LocalDateTime.now());
        codingSubmissionRepository.save(sub);
    }

    // =====================================================================
    // Student — mock tests
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<MockTestDto> listAvailableTests(User user) {
        Student student = requireStudent(user);
        List<MockTestAttempt> mine = mockTestAttemptRepository
                .findByStudent_IdAndSubmittedAtNotNullOrderByCreatedAtDesc(student.getId());
        Map<Long, List<MockTestAttempt>> byTest = mine.stream()
                .collect(Collectors.groupingBy(a -> a.getMockTest().getId()));
        return mockTestRepository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .map(t -> toTestDto(t, byTest.get(t.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MockTestTakeDto getTestToTake(User user, Long testId) {
        Student student = requireStudent(user);
        MockTest test = requirePublishedTest(testId);
        List<PracticeQuestion> questions = test.getQuestions().stream()
                .filter(PracticeQuestion::isPublished)
                .collect(Collectors.toList());
        if (questions.isEmpty()) {
            throw new BusinessException("This test has no questions yet.");
        }
        // Register (or resume) an in-progress attempt so the clock is server-authoritative.
        mockTestAttemptRepository
                .findFirstByMockTest_IdAndStudent_IdAndSubmittedAtIsNull(test.getId(), student.getId())
                .orElseGet(() -> mockTestAttemptRepository.save(MockTestAttempt.builder()
                        .mockTest(test)
                        .student(student)
                        .passed(false)
                        .startedAt(LocalDateTime.now())
                        .build()));

        MockTestTakeDto dto = new MockTestTakeDto()
                .setId(test.getId())
                .setTitle(test.getTitle())
                .setDescription(test.getDescription())
                .setDurationMinutes(test.getDurationMinutes())
                .setPassingMarks(test.getPassingMarks())
                .setTotalMarks(questions.stream().mapToInt(this::safeMarks).sum());
        dto.setQuestions(questions.stream().map(this::toTakeDto).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional
    public MockTestResultDto submitTest(User user, Long testId, Map<Long, String> answers) {
        Student student = requireStudent(user);
        MockTest test = requirePublishedTest(testId);
        MockTestAttempt attempt = mockTestAttemptRepository
                .findFirstByMockTest_IdAndStudent_IdAndSubmittedAtIsNull(test.getId(), student.getId())
                .orElseThrow(() -> new BusinessException("This test attempt has already been submitted."));

        enforceTimeLimit(test, attempt);

        int score = 0;
        int totalMarks = 0;
        int correct = 0;
        List<PracticeQuestion> questions = test.getQuestions().stream()
                .filter(PracticeQuestion::isPublished)
                .collect(Collectors.toList());
        for (PracticeQuestion q : questions) {
            int marks = safeMarks(q);
            totalMarks += marks;
            String selected = answers.get(q.getId());
            boolean isCorrect = selected != null && q.getCorrectOption() != null
                    && selected.trim().equalsIgnoreCase(q.getCorrectOption().trim());
            if (isCorrect) {
                correct++;
                score += marks;
            }
            MockTestAnswer answer = MockTestAnswer.builder()
                    .attempt(attempt)
                    .question(q)
                    .selectedOption(selected == null ? null : selected.trim().toUpperCase())
                    .correct(isCorrect)
                    .marksAwarded(isCorrect ? marks : 0)
                    .build();
            attempt.getAnswers().add(answer);
        }

        int percentage = totalMarks > 0 ? (int) Math.round(score * 100.0 / totalMarks) : 0;
        int passingMarks = test.getPassingMarks() != null ? test.getPassingMarks()
                : (int) Math.ceil(totalMarks * 0.5);

        attempt.setScoreObtained(score);
        attempt.setTotalMarks(totalMarks);
        attempt.setPercentage(percentage);
        attempt.setCorrectCount(correct);
        attempt.setTotalQuestions(questions.size());
        attempt.setPassed(score >= passingMarks && totalMarks > 0);
        attempt.setSubmittedAt(LocalDateTime.now());
        mockTestAttemptRepository.save(attempt);

        return toTestResult(attempt, test.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public MockTestResultDto getTestResult(User user, Long attemptId) {
        Student student = requireStudent(user);
        MockTestAttempt attempt = mockTestAttemptRepository.findByIdAndStudent_Id(attemptId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Test result not found."));
        return toTestResult(attempt, attempt.getMockTest().getTitle());
    }

    // =====================================================================
    // Student — performance
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public PracticePerformanceDto performance(User user) {
        Student student = requireStudent(user);
        PracticePerformanceDto dto = new PracticePerformanceDto();

        List<PracticeAttempt> attempts = practiceAttemptRepository
                .findByStudent_IdOrderByCreatedAtDesc(student.getId());
        long total = attempts.size();
        long correct = attempts.stream().filter(PracticeAttempt::isCorrect).count();
        dto.setTotalAttempts(total);
        dto.setCorrectAttempts(correct);
        dto.setAccuracyPercent(total > 0 ? (int) Math.round(correct * 100.0 / total) : 0);

        Map<QuestionCategory, List<PracticeAttempt>> grouped = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getCategory()));
        for (QuestionCategory c : QuestionCategory.values()) {
            List<PracticeAttempt> list = grouped.get(c);
            if (list == null || list.isEmpty()) {
                continue;
            }
            long cc = list.stream().filter(PracticeAttempt::isCorrect).count();
            dto.getByCategory().add(new PracticePerformanceDto.CategoryStat()
                    .setCategory(categoryLabel(c.name()))
                    .setAttempts(list.size())
                    .setCorrect(cc)
                    .setAccuracyPercent((int) Math.round(cc * 100.0 / list.size())));
        }
        attempts.stream().limit(10).forEach(a -> dto.getRecentAttempts().add(
                new PracticePerformanceDto.RecentAttempt()
                        .setQuestionText(truncate(a.getQuestion().getQuestionText(), 120))
                        .setCategoryLabel(categoryLabel(a.getQuestion().getCategory().name()))
                        .setCorrect(a.isCorrect())
                        .setAttemptedAt(a.getAttemptedAt())));

        List<MockTestAttempt> testAttempts = mockTestAttemptRepository
                .findByStudent_IdAndSubmittedAtNotNullOrderByCreatedAtDesc(student.getId());
        dto.setMockTestsTaken(testAttempts.size());
        dto.setMockTestsPassed(testAttempts.stream().filter(MockTestAttempt::isPassed).count());
        testAttempts.stream().map(MockTestAttempt::getPercentage).filter(Objects::nonNull)
                .max(Integer::compareTo).ifPresent(dto::setBestTestPercent);
        testAttempts.stream().limit(5).forEach(a -> dto.getRecentTests().add(
                new PracticePerformanceDto.RecentTest()
                        .setTitle(a.getMockTest().getTitle())
                        .setPercentage(a.getPercentage())
                        .setPassed(a.isPassed())
                        .setSubmittedAt(a.getSubmittedAt())));
        return dto;
    }

    // =====================================================================
    // Mapping / validation helpers
    // =====================================================================
    private void applyQuestionFields(PracticeQuestion q, PracticeQuestionFormDto form) {
        QuestionType type = parseType(form.getType());
        QuestionCategory category = parseCategory(form.getCategory());
        QuestionDifficulty difficulty = parseDifficulty(form.getDifficulty());

        q.setType(type);
        q.setCategory(category);
        q.setDifficulty(difficulty);
        q.setTopic(trimToNull(form.getTopic()));
        q.setQuestionText(form.getQuestionText() == null ? null : form.getQuestionText().trim());
        q.setPublished(form.isPublished());
        q.setMarks(form.getMarks() == null ? 1 : form.getMarks());

        if (type == QuestionType.MCQ) {
            q.setOptionA(trimToNull(form.getOptionA()));
            q.setOptionB(trimToNull(form.getOptionB()));
            q.setOptionC(trimToNull(form.getOptionC()));
            q.setOptionD(trimToNull(form.getOptionD()));
            q.setExplanation(trimToNull(form.getExplanation()));
            String correct = form.getCorrectOption() == null ? null : form.getCorrectOption().trim().toUpperCase();
            validateMcq(q, correct);
            q.setCorrectOption(correct);
            // Clear coding-only fields so a converted question is clean.
            q.setInputFormat(null);
            q.setOutputFormat(null);
            q.setConstraintsText(null);
            q.setExamples(null);
            q.setTags(null);
        } else {
            q.setInputFormat(trimToNull(form.getInputFormat()));
            q.setOutputFormat(trimToNull(form.getOutputFormat()));
            q.setConstraintsText(trimToNull(form.getConstraintsText()));
            q.setExamples(trimToNull(form.getExamples()));
            q.setTags(trimToNull(form.getTags()));
            // Clear MCQ-only fields.
            q.setOptionA(null);
            q.setOptionB(null);
            q.setOptionC(null);
            q.setOptionD(null);
            q.setCorrectOption(null);
            q.setExplanation(null);
        }
    }

    private void validateMcq(PracticeQuestion q, String correct) {
        if (correct == null || correct.isBlank()) {
            throw new BusinessException("MCQ questions must have a correct option.");
        }
        String optionText = switch (correct) {
            case "A" -> q.getOptionA();
            case "B" -> q.getOptionB();
            case "C" -> q.getOptionC();
            case "D" -> q.getOptionD();
            default -> null;
        };
        if (optionText == null || optionText.isBlank()) {
            throw new BusinessException("Correct option '" + correct + "' has no matching answer text.");
        }
    }

    private void applyTestFields(MockTest t, MockTestFormDto form) {
        t.setTitle(form.getTitle().trim());
        t.setDescription(trimToNull(form.getDescription()));
        t.setDurationMinutes(form.getDurationMinutes());
        t.setPassingMarks(form.getPassingMarks());
        t.setPublished(form.isPublished());

        List<Long> ids = form.getQuestionIds() == null ? List.of() : form.getQuestionIds();
        List<PracticeQuestion> selected = questionRepository.findAllById(ids).stream()
                .filter(PracticeQuestion::isPublished)
                .peek(q -> {
                    if (q.isCoding()) {
                        throw new BusinessException("Coding problems cannot be added to a mock test.");
                    }
                })
                .collect(Collectors.toList());
        if (selected.isEmpty()) {
            throw new BusinessException("Select at least one question for the test.");
        }
        t.getQuestions().clear();
        t.getQuestions().addAll(selected);
    }

    private void enforceTimeLimit(MockTest test, MockTestAttempt attempt) {
        if (test.getDurationMinutes() == null || test.getDurationMinutes() <= 0 || attempt.getStartedAt() == null) {
            return; // untimed test
        }
        long elapsed = Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
        long allowed = test.getDurationMinutes() * 60L + TIME_GRACE_SECONDS;
        if (elapsed > allowed) {
            throw new BusinessException("The time limit for this test has elapsed.");
        }
    }

    private PracticeQuestionDto toQuestionDto(PracticeQuestion q) {
        return new PracticeQuestionDto()
                .setId(q.getId())
                .setType(q.getType().name())
                .setCategory(q.getCategory().name())
                .setCategoryLabel(categoryLabel(q.getCategory().name()))
                .setTopic(q.getTopic())
                .setDifficulty(q.getDifficulty().name())
                .setQuestionText(q.getQuestionText())
                .setOptionA(q.getOptionA())
                .setOptionB(q.getOptionB())
                .setOptionC(q.getOptionC())
                .setOptionD(q.getOptionD())
                .setCorrectOption(q.getCorrectOption())
                .setExplanation(q.getExplanation())
                .setMarks(q.getMarks())
                .setPublished(q.isPublished())
                .setInputFormat(q.getInputFormat())
                .setOutputFormat(q.getOutputFormat())
                .setConstraintsText(q.getConstraintsText())
                .setExamples(q.getExamples())
                .setTags(q.getTags());
    }

    private PracticeQuestionTakeDto toTakeDto(PracticeQuestion q) {
        return new PracticeQuestionTakeDto()
                .setId(q.getId())
                .setCategory(q.getCategory().name())
                .setCategoryLabel(categoryLabel(q.getCategory().name()))
                .setTopic(q.getTopic())
                .setDifficulty(q.getDifficulty().name())
                .setQuestionText(q.getQuestionText())
                .setOptionA(q.getOptionA())
                .setOptionB(q.getOptionB())
                .setOptionC(q.getOptionC())
                .setOptionD(q.getOptionD())
                .setMarks(q.getMarks());
    }

    private MockTestDto toTestDto(MockTest t, List<MockTestAttempt> myAttempts) {
        List<PracticeQuestion> qs = t.getQuestions();
        MockTestDto dto = new MockTestDto()
                .setId(t.getId())
                .setTitle(t.getTitle())
                .setDescription(t.getDescription())
                .setDurationMinutes(t.getDurationMinutes())
                .setPassingMarks(t.getPassingMarks())
                .setPublished(t.isPublished())
                .setQuestionCount(qs.size())
                .setTotalMarks(qs.stream().mapToInt(this::safeMarks).sum());
        // Total completed attempts across all students (admin listing).
        dto.setAttemptCount((int) mockTestAttemptRepository
                .findByMockTest_IdAndSubmittedAtNotNullOrderByCreatedAtDesc(t.getId()).size());
        if (myAttempts != null) {
            dto.setMyAttempts(myAttempts.size());
            myAttempts.stream().map(MockTestAttempt::getPercentage).filter(Objects::nonNull)
                    .max(Integer::compareTo).ifPresent(dto::setMyBestPercent);
        }
        return dto;
    }

    private MockTestResultDto toTestResult(MockTestAttempt attempt, String title) {
        MockTestResultDto dto = new MockTestResultDto()
                .setAttemptId(attempt.getId())
                .setTitle(title)
                .setScoreObtained(attempt.getScoreObtained())
                .setTotalMarks(attempt.getTotalMarks())
                .setPercentage(attempt.getPercentage())
                .setCorrectCount(attempt.getCorrectCount())
                .setTotalQuestions(attempt.getTotalQuestions())
                .setPassed(attempt.isPassed())
                .setSubmittedAt(attempt.getSubmittedAt());
        for (MockTestAnswer a : mockTestAnswerRepository.findWithQuestionsByAttempt(attempt.getId())) {
            PracticeQuestion q = a.getQuestion();
            dto.getReview().add(new MockTestResultDto.ReviewRow()
                    .setQuestionText(q.getQuestionText())
                    .setSelectedOption(a.getSelectedOption())
                    .setCorrectOption(q.getCorrectOption())
                    .setCorrect(a.isCorrect())
                    .setMarksAwarded(a.getMarksAwarded())
                    .setExplanation(q.getExplanation()));
        }
        return dto;
    }

    // ----- import parsers -----
    @SuppressWarnings("unchecked")
    private List<PracticeQuestion> parseJsonQuestions(String payload) {
        List<PracticeQuestion> result = new ArrayList<>();
        String trimmed = payload.trim();
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            if (trimmed.startsWith("[")) {
                rows.addAll(objectMapper.readValue(trimmed, List.class));
            } else {
                for (String line : payload.split("\\R")) {
                    if (!line.isBlank()) {
                        rows.add(objectMapper.readValue(line, Map.class));
                    }
                }
            }
        } catch (Exception ex) {
            throw new BusinessException("Could not parse JSON: " + ex.getMessage());
        }
        for (Map<String, Object> row : rows) {
            PracticeQuestionFormDto form = new PracticeQuestionFormDto()
                    .setType(str(row.get("type")))
                    .setCategory(str(row.get("category")))
                    .setTopic(str(row.get("topic")))
                    .setDifficulty(str(row.get("difficulty")))
                    .setQuestionText(str(row.get("question")))
                    .setOptionA(str(row.get("optionA")))
                    .setOptionB(str(row.get("optionB")))
                    .setOptionC(str(row.get("optionC")))
                    .setOptionD(str(row.get("optionD")))
                    .setCorrectOption(str(row.get("correctOption")))
                    .setExplanation(str(row.get("explanation")))
                    .setPublished(true);
            Object marks = row.get("marks");
            if (marks instanceof Number n) {
                form.setMarks(n.intValue());
            }
            PracticeQuestion q = new PracticeQuestion();
            applyQuestionFields(q, form);
            result.add(q);
        }
        return result;
    }

    private List<PracticeQuestion> parseCsvQuestions(String payload) {
        List<PracticeQuestion> result = new ArrayList<>();
        String[] lines = payload.split("\\R");
        int start = 0;
        if (lines.length > 0 && lines[0].toLowerCase().contains("category")) {
            start = 1; // skip header
        }
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            String[] c = line.split(",", -1);
            if (c.length < 9) {
                throw new BusinessException("Line " + (i + 1) + ": expected at least 9 columns.");
            }
            PracticeQuestionFormDto form = new PracticeQuestionFormDto()
                    .setType("MCQ")
                    .setCategory(c[0])
                    .setTopic(c[1])
                    .setDifficulty(c[2])
                    .setQuestionText(c[3])
                    .setOptionA(c[4])
                    .setOptionB(c[5])
                    .setOptionC(c[6])
                    .setOptionD(c[7])
                    .setCorrectOption(c[8])
                    .setExplanation(c.length > 9 ? c[9] : null)
                    .setPublished(true);
            PracticeQuestion q = new PracticeQuestion();
            applyQuestionFields(q, form);
            result.add(q);
        }
        return result;
    }

    // ----- guards & misc -----
    private PracticeQuestion requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found."));
    }

    private MockTest requireTest(Long id) {
        return mockTestRepository.findWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mock test not found."));
    }

    private MockTest requirePublishedTest(Long id) {
        MockTest t = requireTest(id);
        if (!t.isPublished()) {
            throw new BusinessException("This test is not available yet.");
        }
        return t;
    }

    private void requirePublishedMcq(PracticeQuestion q) {
        if (q.isCoding() || !q.isPublished()) {
            throw new BusinessException("This question is not available for practice.");
        }
    }

    private Student requireStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Student access only.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));
    }

    private QuestionType parseType(String s) {
        if (isBlank(s)) {
            return QuestionType.MCQ;
        }
        try {
            return QuestionType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid question type: " + s);
        }
    }

    private QuestionCategory parseCategory(String s) {
        if (isBlank(s)) {
            throw new BusinessException("Category is required.");
        }
        try {
            return QuestionCategory.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid category: " + s);
        }
    }

    private QuestionDifficulty parseDifficulty(String s) {
        if (isBlank(s)) {
            return QuestionDifficulty.EASY;
        }
        try {
            return QuestionDifficulty.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid difficulty: " + s);
        }
    }

    private int safeMarks(PracticeQuestion q) {
        return q.getMarks() == null ? 1 : Math.max(0, q.getMarks());
    }

    private static String categoryLabel(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
