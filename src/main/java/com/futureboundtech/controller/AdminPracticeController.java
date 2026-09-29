package com.futureboundtech.controller;

import com.futureboundtech.dto.MockTestFormDto;
import com.futureboundtech.dto.PracticeQuestionFormDto;
import com.futureboundtech.enums.QuestionCategory;
import com.futureboundtech.enums.QuestionDifficulty;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin management of the Phase 16 practice platform: the question bank
 * (MCQ + coding), bulk import, and mock tests. Scoped to /admin/** (ROLE ADMIN).
 */
@Controller
@RequestMapping("/admin/practice")
@RequiredArgsConstructor
public class AdminPracticeController {

    private final PracticeService practiceService;

    // ------------------------------------------------------------- questions
    @GetMapping({"", "/questions"})
    public String questions(@RequestParam(required = false) String type,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String difficulty,
                            @RequestParam(required = false) String topic,
                            @RequestParam(required = false) String query,
                            Model model) {
        model.addAttribute("pageTitle", "Practice Questions — Admin");
        model.addAttribute("questions", practiceService.listQuestions(type, category, difficulty, topic, query));
        populateFilters(model, type, category, difficulty, topic, query);
        return "admin/practice-questions";
    }

    @GetMapping("/questions/new")
    public String newQuestionForm(Model model) {
        populateQuestionForm(model, new PracticeQuestionFormDto().setPublished(true).setType("MCQ")
                .setDifficulty("EASY"), false, null);
        return "admin/practice-question-form";
    }

    @GetMapping("/questions/{id}/edit")
    public String editQuestionForm(@PathVariable Long id, Model model) {
        populateQuestionForm(model, questionToForm(practiceService.getQuestion(id)), true, id);
        return "admin/practice-question-form";
    }

    @PostMapping("/questions")
    public String createQuestion(@Valid @ModelAttribute("questionForm") PracticeQuestionFormDto form,
                                 BindingResult bindingResult, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateQuestionForm(model, form, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/practice-question-form";
        }
        try {
            practiceService.createQuestion(form);
            redirectAttributes.addFlashAttribute("successMessage", "Question added.");
            return "redirect:/admin/practice/questions";
        } catch (BusinessException ex) {
            populateQuestionForm(model, form, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/practice-question-form";
        }
    }

    @PostMapping("/questions/{id}")
    public String updateQuestion(@PathVariable Long id,
                                 @Valid @ModelAttribute("questionForm") PracticeQuestionFormDto form,
                                 BindingResult bindingResult, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateQuestionForm(model, form, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/practice-question-form";
        }
        try {
            practiceService.updateQuestion(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Question updated.");
            return "redirect:/admin/practice/questions";
        } catch (BusinessException ex) {
            populateQuestionForm(model, form, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/practice-question-form";
        }
    }

    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.deleteQuestion(id);
        redirectAttributes.addFlashAttribute("successMessage", "Question deleted.");
        return "redirect:/admin/practice/questions";
    }

    @PostMapping("/questions/{id}/publish")
    public String publishQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.setQuestionPublished(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Question published.");
        return "redirect:/admin/practice/questions";
    }

    @PostMapping("/questions/{id}/unpublish")
    public String unpublishQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.setQuestionPublished(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Question moved to draft.");
        return "redirect:/admin/practice/questions";
    }

    // ------------------------------------------------------------- import
    @GetMapping("/questions/import")
    public String importForm(Model model) {
        model.addAttribute("pageTitle", "Import Questions — Admin");
        return "admin/practice-import";
    }

    @PostMapping("/questions/import")
    public String importQuestions(@RequestParam String payload,
                                  @RequestParam(defaultValue = "json") String format,
                                  Model model, RedirectAttributes redirectAttributes) {
        try {
            int count = practiceService.importQuestions(payload, format);
            redirectAttributes.addFlashAttribute("successMessage", count + " question(s) imported.");
            return "redirect:/admin/practice/questions";
        } catch (BusinessException ex) {
            model.addAttribute("pageTitle", "Import Questions — Admin");
            model.addAttribute("payload", payload);
            model.addAttribute("format", format);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/practice-import";
        }
    }

    // ------------------------------------------------------------- mock tests
    @GetMapping("/tests")
    public String tests(Model model) {
        model.addAttribute("pageTitle", "Mock Tests — Admin");
        model.addAttribute("tests", practiceService.listTests());
        return "admin/practice-tests";
    }

    @GetMapping("/tests/new")
    public String newTestForm(Model model) {
        populateTestForm(model, new MockTestFormDto().setPublished(true), false, null);
        return "admin/practice-test-form";
    }

    @GetMapping("/tests/{id}/edit")
    public String editTestForm(@PathVariable Long id, Model model) {
        populateTestForm(model, practiceService.getTestForm(id), true, id);
        return "admin/practice-test-form";
    }

    @PostMapping("/tests")
    public String createTest(@Valid @ModelAttribute("testForm") MockTestFormDto form,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateTestForm(model, form, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/practice-test-form";
        }
        try {
            practiceService.createTest(form);
            redirectAttributes.addFlashAttribute("successMessage", "Mock test created.");
            return "redirect:/admin/practice/tests";
        } catch (BusinessException ex) {
            populateTestForm(model, form, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/practice-test-form";
        }
    }

    @PostMapping("/tests/{id}")
    public String updateTest(@PathVariable Long id,
                             @Valid @ModelAttribute("testForm") MockTestFormDto form,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateTestForm(model, form, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/practice-test-form";
        }
        try {
            practiceService.updateTest(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Mock test updated.");
            return "redirect:/admin/practice/tests";
        } catch (BusinessException ex) {
            populateTestForm(model, form, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/practice-test-form";
        }
    }

    @PostMapping("/tests/{id}/delete")
    public String deleteTest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.deleteTest(id);
        redirectAttributes.addFlashAttribute("successMessage", "Mock test deleted.");
        return "redirect:/admin/practice/tests";
    }

    @PostMapping("/tests/{id}/publish")
    public String publishTest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.setTestPublished(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Mock test published.");
        return "redirect:/admin/practice/tests";
    }

    @PostMapping("/tests/{id}/unpublish")
    public String unpublishTest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        practiceService.setTestPublished(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Mock test moved to draft.");
        return "redirect:/admin/practice/tests";
    }

    // ------------------------------------------------------------- helpers
    private void populateFilters(Model model, String type, String category, String difficulty,
                                 String topic, String query) {
        model.addAttribute("categories", categoryOptions());
        model.addAttribute("difficulties", difficultyOptions());
        model.addAttribute("topics", practiceService.listTopics());
        model.addAttribute("fType", type);
        model.addAttribute("fCategory", category);
        model.addAttribute("fDifficulty", difficulty);
        model.addAttribute("fTopic", topic);
        model.addAttribute("fQuery", query);
    }

    private void populateQuestionForm(Model model, PracticeQuestionFormDto form, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Question" : "New Question") + " — Admin");
        model.addAttribute("questionForm", form);
        model.addAttribute("editing", editing);
        model.addAttribute("formAction", editing ? "/admin/practice/questions/" + id : "/admin/practice/questions");
        model.addAttribute("categories", categoryOptions());
        model.addAttribute("difficulties", difficultyOptions());
    }

    private void populateTestForm(Model model, MockTestFormDto form, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Mock Test" : "New Mock Test") + " — Admin");
        model.addAttribute("testForm", form);
        model.addAttribute("editing", editing);
        model.addAttribute("formAction", editing ? "/admin/practice/tests/" + id : "/admin/practice/tests");
        model.addAttribute("allQuestions", practiceService.listMcqQuestionsForPicker());
        model.addAttribute("selectedIds", form.getQuestionIds());
    }

    private PracticeQuestionFormDto questionToForm(com.futureboundtech.dto.PracticeQuestionDto d) {
        return new PracticeQuestionFormDto()
                .setId(d.getId())
                .setType(d.getType())
                .setCategory(d.getCategory())
                .setTopic(d.getTopic())
                .setDifficulty(d.getDifficulty())
                .setQuestionText(d.getQuestionText())
                .setOptionA(d.getOptionA())
                .setOptionB(d.getOptionB())
                .setOptionC(d.getOptionC())
                .setOptionD(d.getOptionD())
                .setCorrectOption(d.getCorrectOption())
                .setExplanation(d.getExplanation())
                .setMarks(d.getMarks())
                .setPublished(d.isPublished())
                .setInputFormat(d.getInputFormat())
                .setOutputFormat(d.getOutputFormat())
                .setConstraintsText(d.getConstraintsText())
                .setExamples(d.getExamples())
                .setTags(d.getTags());
    }

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
