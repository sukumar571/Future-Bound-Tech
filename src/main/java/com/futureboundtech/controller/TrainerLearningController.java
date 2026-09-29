package com.futureboundtech.controller;

import com.futureboundtech.dto.AssignmentFormDto;
import com.futureboundtech.dto.QuizFormDto;
import com.futureboundtech.dto.QuizQuestionFormDto;
import com.futureboundtech.dto.TrainerSubmissionDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.TrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/trainer")
public class TrainerLearningController {

    private final TrainerService trainerService;
    private final CourseService courseService;
    private final FileStorageService fileStorageService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
        binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
    }

    // ==================== Assignments ====================

    @GetMapping("/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Assignments — Trainer");
        model.addAttribute("assignments", trainerService.listAssignments(principal.getUser()));
        return "trainer/assignments";
    }

    @GetMapping("/assignments/new")
    public String newAssignment(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        populateAssignmentForm(model, new AssignmentFormDto(), principal, "/trainer/assignments/new", false, null);
        return "trainer/assignment-form";
    }

    @PostMapping("/assignments/new")
    public String createAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                   @Valid @ModelAttribute("assignmentForm") AssignmentFormDto form,
                                   BindingResult bindingResult,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            populateAssignmentForm(model, form, principal, "/trainer/assignments/new", false, null);
            return "trainer/assignment-form";
        }
        trainerService.createAssignment(principal.getUser(), form);
        return "redirect:/trainer/assignments";
    }

    @GetMapping("/assignments/{id}/edit")
    public String editAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable Long id, Model model) {
        AssignmentFormDto form = trainerService.getAssignment(principal.getUser(), id);
        populateAssignmentForm(model, form, principal, "/trainer/assignments/" + id + "/edit", true, id);
        return "trainer/assignment-form";
    }

    @PostMapping("/assignments/{id}/edit")
    public String updateAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable Long id,
                                   @Valid @ModelAttribute("assignmentForm") AssignmentFormDto form,
                                   BindingResult bindingResult,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            populateAssignmentForm(model, form, principal, "/trainer/assignments/" + id + "/edit", true, id);
            return "trainer/assignment-form";
        }
        trainerService.updateAssignment(principal.getUser(), id, form);
        return "redirect:/trainer/assignments";
    }

    @PostMapping("/assignments/{id}/delete")
    public String deleteAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable Long id, RedirectAttributes ra) {
        trainerService.deleteAssignment(principal.getUser(), id);
        ra.addFlashAttribute("successMessage", "Assignment deleted.");
        return "redirect:/trainer/assignments";
    }

    @GetMapping("/assignments/{id}/submissions")
    public String submissions(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Submissions — Trainer");
        model.addAttribute("assignment", trainerService.getAssignment(principal.getUser(), id));
        model.addAttribute("submissions", trainerService.listSubmissions(principal.getUser(), id));
        return "trainer/submissions";
    }

    @PostMapping("/submissions/{id}/grade")
    public String gradeSubmission(@AuthenticationPrincipal CustomUserDetails principal,
                                  @PathVariable Long id,
                                  @RequestParam(required = false) String score,
                                  @RequestParam(required = false) String feedback,
                                  @RequestParam(required = false) String status,
                                  @RequestParam Long assignmentId,
                                  RedirectAttributes ra) {
        Integer parsedScore = (score == null || score.isBlank()) ? null : Integer.valueOf(score.trim());
        trainerService.gradeSubmission(principal.getUser(), id, parsedScore, feedback, status);
        ra.addFlashAttribute("successMessage", "Submission saved.");
        return "redirect:/trainer/assignments/" + assignmentId + "/submissions";
    }

    // ==================== Quizzes ====================

    @GetMapping("/quizzes")
    public String quizzes(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Quizzes — Trainer");
        model.addAttribute("quizzes", trainerService.listQuizzes(principal.getUser()));
        return "trainer/quizzes";
    }

    @GetMapping("/quizzes/new")
    public String newQuiz(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        populateQuizForm(model, new QuizFormDto(), principal, "/trainer/quizzes/new", false, null);
        return "trainer/quiz-form";
    }

    @PostMapping("/quizzes/new")
    public String createQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                             @Valid @ModelAttribute("quizForm") QuizFormDto form,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateQuizForm(model, form, principal, "/trainer/quizzes/new", false, null);
            return "trainer/quiz-form";
        }
        trainerService.createQuiz(principal.getUser(), form);
        return "redirect:/trainer/quizzes";
    }

    @GetMapping("/quizzes/{id}/edit")
    public String editQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                           @PathVariable Long id, Model model) {
        populateQuizForm(model, trainerService.getQuiz(principal.getUser(), id), principal,
                "/trainer/quizzes/" + id + "/edit", true, id);
        return "trainer/quiz-form";
    }

    @PostMapping("/quizzes/{id}/edit")
    public String updateQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("quizForm") QuizFormDto form,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateQuizForm(model, form, principal, "/trainer/quizzes/" + id + "/edit", true, id);
            return "trainer/quiz-form";
        }
        trainerService.updateQuiz(principal.getUser(), id, form);
        return "redirect:/trainer/quizzes";
    }

    @PostMapping("/quizzes/{id}/delete")
    public String deleteQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Long id, RedirectAttributes ra) {
        trainerService.deleteQuiz(principal.getUser(), id);
        ra.addFlashAttribute("successMessage", "Quiz deleted.");
        return "redirect:/trainer/quizzes";
    }

    @PostMapping("/quizzes/{id}/publish")
    public String publishQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id, RedirectAttributes ra) {
        trainerService.toggleQuizPublished(principal.getUser(), id, true);
        ra.addFlashAttribute("successMessage", "Quiz published — students can now attempt it.");
        return "redirect:/trainer/quizzes";
    }

    @PostMapping("/quizzes/{id}/unpublish")
    public String unpublishQuiz(@AuthenticationPrincipal CustomUserDetails principal,
                                @PathVariable Long id, RedirectAttributes ra) {
        trainerService.toggleQuizPublished(principal.getUser(), id, false);
        ra.addFlashAttribute("infoMessage", "Quiz moved to draft.");
        return "redirect:/trainer/quizzes";
    }

    @GetMapping("/quizzes/{id}/questions")
    public String questions(@AuthenticationPrincipal CustomUserDetails principal,
                            @PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Quiz Questions — Trainer");
        model.addAttribute("quiz", trainerService.getQuiz(principal.getUser(), id));
        model.addAttribute("questions", trainerService.listQuestions(principal.getUser(), id));
        return "trainer/quiz-questions";
    }

    @GetMapping("/quizzes/{id}/questions/new")
    public String newQuestion(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id, Model model) {
        QuizFormDto quiz = trainerService.getQuiz(principal.getUser(), id);
        QuizQuestionFormDto form = new QuizQuestionFormDto().setQuizId(id);
        populateQuestionForm(model, form, quiz, "/trainer/quizzes/" + id + "/questions/new", false, null);
        return "trainer/quiz-question-form";
    }

    @PostMapping("/quizzes/{id}/questions/new")
    public String createQuestion(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable Long id,
                                 @Valid @ModelAttribute("questionForm") QuizQuestionFormDto form,
                                 BindingResult bindingResult, Model model) {
        QuizFormDto quiz = trainerService.getQuiz(principal.getUser(), id);
        if (bindingResult.hasErrors()) {
            populateQuestionForm(model, form, quiz, "/trainer/quizzes/" + id + "/questions/new", false, null);
            return "trainer/quiz-question-form";
        }
        trainerService.addQuestion(principal.getUser(), id, form);
        return "redirect:/trainer/quizzes/" + id + "/questions";
    }

    @GetMapping("/questions/{qid}/edit")
    public String editQuestion(@AuthenticationPrincipal CustomUserDetails principal,
                               @PathVariable Long qid, Model model) {
        QuizQuestionFormDto form = trainerService.getQuestion(principal.getUser(), qid);
        QuizFormDto quiz = trainerService.getQuiz(principal.getUser(), form.getQuizId());
        populateQuestionForm(model, form, quiz, "/trainer/questions/" + qid + "/edit", true, qid);
        return "trainer/quiz-question-form";
    }

    @PostMapping("/questions/{qid}/edit")
    public String updateQuestion(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable Long qid,
                                 @Valid @ModelAttribute("questionForm") QuizQuestionFormDto form,
                                 BindingResult bindingResult, Model model) {
        QuizQuestionFormDto existing = trainerService.getQuestion(principal.getUser(), qid);
        QuizFormDto quiz = trainerService.getQuiz(principal.getUser(), existing.getQuizId());
        if (bindingResult.hasErrors()) {
            populateQuestionForm(model, form, quiz, "/trainer/questions/" + qid + "/edit", true, qid);
            return "trainer/quiz-question-form";
        }
        trainerService.updateQuestion(principal.getUser(), qid, form);
        return "redirect:/trainer/quizzes/" + existing.getQuizId() + "/questions";
    }

    @PostMapping("/questions/{qid}/delete")
    public String deleteQuestion(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable Long qid, RedirectAttributes ra) {
        QuizQuestionFormDto existing = trainerService.getQuestion(principal.getUser(), qid);
        trainerService.deleteQuestion(principal.getUser(), qid);
        ra.addFlashAttribute("successMessage", "Question deleted.");
        return "redirect:/trainer/quizzes/" + existing.getQuizId() + "/questions";
    }

    @GetMapping("/quizzes/{id}/results")
    public String results(@AuthenticationPrincipal CustomUserDetails principal,
                          @PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Quiz Results — Trainer");
        model.addAttribute("quiz", trainerService.getQuiz(principal.getUser(), id));
        model.addAttribute("results", trainerService.quizResults(principal.getUser(), id));
        return "trainer/quiz-results";
    }

    // ==================== Helpers ====================

    private void populateAssignmentForm(Model model, AssignmentFormDto form,
                                        CustomUserDetails principal, String action, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit" : "New") + " Assignment — Trainer");
        model.addAttribute("assignmentForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("editing", editing);
        model.addAttribute("assignmentId", id);
        model.addAttribute("batches", trainerService.listBatches(principal.getUser()));
    }

    private void populateQuizForm(Model model, QuizFormDto form,
                                  CustomUserDetails principal, String action, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit" : "New") + " Quiz — Trainer");
        model.addAttribute("quizForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("editing", editing);
        model.addAttribute("quizId", id);
        model.addAttribute("courses", courseService.findCoursesForTrainer(principal.getUser()));
    }

    private void populateQuestionForm(Model model, QuizQuestionFormDto form, QuizFormDto quiz,
                                      String action, boolean editing, Long qid) {
        model.addAttribute("pageTitle", (editing ? "Edit" : "New") + " Question — Trainer");
        model.addAttribute("questionForm", form);
        model.addAttribute("quiz", quiz);
        model.addAttribute("formAction", action);
        model.addAttribute("editing", editing);
        model.addAttribute("questionId", qid);
    }
}
