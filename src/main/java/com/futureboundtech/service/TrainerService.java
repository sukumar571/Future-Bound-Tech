package com.futureboundtech.service;

import com.futureboundtech.dto.*;
import com.futureboundtech.entity.User;

import java.util.List;

/**
 * Every operation is resolved against the signed-in trainer and scoped strictly to
 * the courses and batches assigned to them. Attempts to touch another trainer's data
 * raise an {@link org.springframework.security.access.AccessDeniedException}.
 */
public interface TrainerService {

    // ---- Dashboard ------------------------------------------------------
    TrainerStatsDto dashboard(User user);

    // ---- Profile --------------------------------------------------------
    TrainerProfileFormDto getProfile(User user);

    void updateProfile(User user, TrainerProfileFormDto form);

    // ---- Batches --------------------------------------------------------
    List<BatchDto> listBatches(User user);

    BatchDto getBatch(User user, Long batchId);

    // ---- Students -------------------------------------------------------
    List<TrainerStudentDto> listStudents(User user, Long batchId, String query);

    TrainerStudentDto getStudent(User user, Long studentId);

    List<StudentProgressDto> studentProgress(User user, Long studentId);

    // ---- Assignments ----------------------------------------------------
    List<AssignmentDto> listAssignments(User user);

    AssignmentFormDto getAssignment(User user, Long id);

    void createAssignment(User user, AssignmentFormDto form);

    void updateAssignment(User user, Long id, AssignmentFormDto form);

    void deleteAssignment(User user, Long id);

    List<TrainerSubmissionDto> listSubmissions(User user, Long assignmentId);

    void gradeSubmission(User user, Long submissionId, Integer score, String feedback, String status);

    /** Authorises a trainer to download a specific submission's file; returns its read model. */
    TrainerSubmissionDto getSubmissionForDownload(User user, Long submissionId);

    // ---- Quizzes --------------------------------------------------------
    List<QuizDto> listQuizzes(User user);

    QuizFormDto getQuiz(User user, Long id);

    void createQuiz(User user, QuizFormDto form);

    void updateQuiz(User user, Long id, QuizFormDto form);

    void deleteQuiz(User user, Long id);

    /** Publishes or reverts a quiz to draft. Unpublished quizzes are hidden from students. */
    void toggleQuizPublished(User user, Long id, boolean published);

    List<QuizQuestionFormDto> listQuestions(User user, Long quizId);

    QuizQuestionFormDto getQuestion(User user, Long questionId);

    void addQuestion(User user, Long quizId, QuizQuestionFormDto form);

    void updateQuestion(User user, Long questionId, QuizQuestionFormDto form);

    void deleteQuestion(User user, Long questionId);

    List<QuizAttemptResultDto> quizResults(User user, Long quizId);

    // ---- Classes --------------------------------------------------------
    List<LiveClassDto> listClasses(User user);

    TrainerClassFormDto getClass(User user, Long id);

    void createClass(User user, TrainerClassFormDto form);

    void updateClass(User user, Long id, TrainerClassFormDto form);

    void deleteClass(User user, Long id);

    TrainerClassRosterDto attendanceRoster(User user, Long classId);

    void markAttendance(User user, Long classId, List<Long> presentStudentIds, String status);

    // ---- Announcements --------------------------------------------------
    List<AnnouncementDto> listAnnouncements(User user);

    void createAnnouncement(User user, AnnouncementDto dto);

    void deleteAnnouncement(User user, Long id);
}
