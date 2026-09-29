package com.futureboundtech.controller;

import com.futureboundtech.dto.MockTestResultDto;
import com.futureboundtech.dto.PracticeQuestionResultDto;
import com.futureboundtech.enums.QuestionCategory;
import com.futureboundtech.enums.QuestionDifficulty;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Student-facing practice platform (Phase 16): MCQ practice with instant feedback,
 * a study-only coding-problem library, timed mock tests and performance tracking.
 * Scoped to /student/** (ROLE STUDENT).
 */
@Controller
@RequestMapping("/student/practice")
@RequiredArgsConstructor
public class StudentPracticeController {

    private final PracticeService practiceService;

    // ------------------------------------------------------------- hub
    @GetMapping({"", "/"})
    public String hub(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("categoryOptions", categoryOptions());
        model.addAttribute("counts", categoryCounts());
        model.addAttribute("performance", practiceService.performance(principal.getUser()));
        return "student/practice";
    }

    // ------------------------------------------------------------- MCQ
    @GetMapping("/mcq")
    public String browseMcq(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String difficulty,
                            @RequestParam(required = false) String topic,
                            Model model) {
        model.addAttribute("pageTitle", "Practice MCQs — Future Bound Tech");
        model.addAttribute("questions", practiceService.browseMcq(category, difficulty, topic));
        model.addAttribute("categoryOptions", categoryOptions());
        model.addAttribute("difficultyOptions", difficultyOptions());
        model.addAttribute("topics", practiceService.listTopics());
        model.addAttribute("fCategory", category);
        model.addAttribute("fDifficulty", difficulty);
        model.addAttribute("fTopic", topic);
        return "student/practice-mcq";
    }

    @GetMapping("/mcq/{id}")
    public String attemptForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("question", practiceService.getQuestionToAttempt(id));
        return "student/practice-attempt";
    }

    @PostMapping("/mcq/{id}")
    public String attempt(@PathVariable Long id,
                          @RequestParam(name = "answer", required = false) String answer,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("question", practiceService.getQuestionToAttempt(id));
        try {
            PracticeQuestionResultDto result =
                    practiceService.attemptQuestion(principal.getUser(), id, answer);
            model.addAttribute("result", result);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "student/practice-attempt";
    }

    // --------------------------------------------------- MCQ sequential runner
    /** One-question-at-a-time practice with a position counter and Back/Next navigation. */
    @GetMapping("/mcq/run")
    public String runMcq(@RequestParam(required = false) String category,
                         @RequestParam(required = false) String difficulty,
                         @RequestParam(required = false) String topic,
                         @RequestParam(defaultValue = "0") int i,
                         Model model) {
        List<Long> ids = practiceService.mcqSequenceIds(category, difficulty, topic);
        if (ids.isEmpty()) {
            return "redirect:/student/practice/mcq";
        }
        int index = clamp(i, ids.size());
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("question", practiceService.getQuestionToAttempt(ids.get(index)));
        addRunContext(model, index, ids.size(), category, difficulty, topic);
        return "student/practice-mcq-run";
    }

    @PostMapping("/mcq/run")
    public String submitRun(@RequestParam("questionId") Long questionId,
                            @RequestParam(name = "answer", required = false) String answer,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String difficulty,
                            @RequestParam(required = false) String topic,
                            @RequestParam(defaultValue = "0") int i,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            Model model) {
        List<Long> ids = practiceService.mcqSequenceIds(category, difficulty, topic);
        int total = Math.max(ids.size(), 1);
        int pos = ids.indexOf(questionId);
        int index = pos >= 0 ? pos : clamp(i, total);
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("question", practiceService.getQuestionToAttempt(questionId));
        addRunContext(model, index, ids.size(), category, difficulty, topic);
        try {
            model.addAttribute("result", practiceService.attemptQuestion(principal.getUser(), questionId, answer));
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "student/practice-mcq-run";
    }

    private void addRunContext(Model model, int index, int total,
                               String category, String difficulty, String topic) {
        model.addAttribute("index", index);
        model.addAttribute("total", total);
        model.addAttribute("hasPrev", index > 0);
        model.addAttribute("hasNext", index < total - 1);
        model.addAttribute("isLast", index >= total - 1);
        model.addAttribute("fCategory", category);
        model.addAttribute("fDifficulty", difficulty);
        model.addAttribute("fTopic", topic);
    }

    private static int clamp(int i, int size) {
        return Math.max(0, Math.min(i, size - 1));
    }

    // ------------------------------------------------------------- coding library
    @GetMapping("/coding")
    public String browseCoding(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String difficulty,
                               @RequestParam(required = false) String topic,
                               Model model) {
        model.addAttribute("pageTitle", "Coding Problems — Future Bound Tech");
        model.addAttribute("problems", practiceService.browseCoding(category, difficulty, topic));
        model.addAttribute("categoryOptions", categoryOptions());
        model.addAttribute("difficultyOptions", difficultyOptions());
        model.addAttribute("fCategory", category);
        model.addAttribute("fDifficulty", difficulty);
        model.addAttribute("fTopic", topic);
        return "student/practice-coding";
    }

    @GetMapping("/coding/{id}")
    public String codingDetail(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {
        model.addAttribute("pageTitle", "Coding Problem — Future Bound Tech");
        model.addAttribute("problem", practiceService.getCodingProblem(id));
        model.addAttribute("solution", practiceService.getSolution(principal.getUser(), id));
        return "student/practice-coding-detail";
    }

    /** Saves the student's own solution draft / solved flag. The code is never executed. */
    @PostMapping("/coding/{id}/solution")
    public String saveSolution(@PathVariable Long id,
                               @RequestParam(name = "code", required = false) String code,
                               @RequestParam(name = "language", required = false) String language,
                               @RequestParam(name = "solved", required = false, defaultValue = "false") boolean solved,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {
        try {
            practiceService.saveSolution(principal.getUser(), id, code, language, solved);
            redirectAttributes.addFlashAttribute("successMessage", "Your solution has been saved.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/practice/coding/" + id;
    }

    // ------------------------------------------------------------- mock tests
    @GetMapping("/tests")
    public String tests(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Mock Tests — Future Bound Tech");
        model.addAttribute("tests", practiceService.listAvailableTests(principal.getUser()));
        return "student/practice-tests";
    }

    @GetMapping("/tests/{id}/take")
    public String takeTest(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {
        model.addAttribute("pageTitle", "Take Test — Future Bound Tech");
        model.addAttribute("test", practiceService.getTestToTake(principal.getUser(), id));
        return "student/practice-test-take";
    }

    @PostMapping("/tests/{id}/submit")
    public String submitTest(@PathVariable Long id,
                             @RequestParam Map<String, String> params,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Map<Long, String> answers = new HashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("answer_")) {
                try {
                    answers.put(Long.valueOf(k.substring("answer_".length())), v);
                } catch (NumberFormatException ignored) {
                    // skip malformed keys
                }
            }
        });
        try {
            MockTestResultDto result = practiceService.submitTest(principal.getUser(), id, answers);
            return "redirect:/student/practice/tests/results/" + result.getAttemptId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Take Test — Future Bound Tech");
            model.addAttribute("test", practiceService.getTestToTake(principal.getUser(), id));
            return "student/practice-test-take";
        }
    }

    @GetMapping("/tests/results/{attemptId}")
    public String testResult(@PathVariable Long attemptId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             Model model) {
        model.addAttribute("pageTitle", "Test Result — Future Bound Tech");
        model.addAttribute("result", practiceService.getTestResult(principal.getUser(), attemptId));
        return "student/practice-test-result";
    }

    // ------------------------------------------------------------- performance
    @GetMapping("/performance")
    public String performance(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Practice — Future Bound Tech");
        model.addAttribute("performance", practiceService.performance(principal.getUser()));
        return "student/practice-performance";
    }

    // ------------------------------------------------------------- helpers
    private Map<String, String> categoryOptions() {
        Map<String, String> map = new LinkedHashMap<>();
        for (QuestionCategory c : QuestionCategory.values()) {
            map.put(c.name(), label(c.name()));
        }
        return map;
    }

    private Map<String, String> difficultyOptions() {
        Map<String, String> map = new LinkedHashMap<>();
        for (QuestionDifficulty d : QuestionDifficulty.values()) {
            map.put(d.name(), label(d.name()));
        }
        return map;
    }

    private Map<String, Long> categoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        practiceService.mcqCategoryCounts().forEach(nv -> counts.put(nv.getName(), nv.getValue()));
        return counts;
    }

    private static String label(String enumName) {
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
}
