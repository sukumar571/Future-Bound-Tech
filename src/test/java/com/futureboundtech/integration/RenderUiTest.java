package com.futureboundtech.integration;

import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Batch;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.LiveClass;
import com.futureboundtech.entity.PracticeQuestion;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.ClassMode;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.QuestionCategory;
import com.futureboundtech.enums.QuestionDifficulty;
import com.futureboundtech.enums.QuestionType;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.repository.BatchRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.LiveClassRepository;
import com.futureboundtech.repository.PracticeQuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Server-side UI rendering tests (Thymeleaf + layout fragments render under
 * MockMvc): navbar links, auth forms, course cards/detail and the role dashboards.
 * These catch broken templates, bad fragment includes and broken links.
 */
class RenderUiTest extends AbstractIntegrationTest {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private PracticeQuestionRepository practiceQuestionRepository;

    private PracticeQuestion seedMcq(String text, String correct, String explanation) {
        return practiceQuestionRepository.save(PracticeQuestion.builder()
                .type(QuestionType.MCQ).category(QuestionCategory.JAVA)
                .difficulty(QuestionDifficulty.EASY)
                .questionText(text)
                .optionA("ArrayList").optionB("LinkedList").optionC("HashSet").optionD("TreeMap")
                .correctOption(correct).explanation(explanation).marks(1).published(true).build());
    }

    private PracticeQuestion seedCodingProblem(String text) {
        return practiceQuestionRepository.save(PracticeQuestion.builder()
                .type(QuestionType.CODING).category(QuestionCategory.JAVA)
                .difficulty(QuestionDifficulty.EASY)
                .questionText(text)
                .inputFormat("A single integer N").outputFormat("The sum 1..N")
                .published(true).build());
    }

    private Course seedPublishedCourse(String slug, String title) {
        return courseRepository.save(Course.builder()
                .title(title).slug(slug)
                .shortDescription("A hands-on track").description("Full description for " + title)
                .category(CourseCategory.WEB_DEVELOPMENT).level(CourseLevel.BEGINNER)
                .durationInDays(45).fee(new BigDecimal("4999")).trainingMode(TrainingMode.ONLINE)
                .status(CourseStatus.PUBLISHED).publicListed(true).build());
    }

    @Test
    @DisplayName("home page renders with a working navbar (brand, courses link, mobile toggler)")
    void homeRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Future Bound Tech")))
                .andExpect(content().string(containsString("/courses")))
                .andExpect(content().string(containsString("navbar-toggler")));
    }

    @Test
    @DisplayName("login form exposes the username and password fields Spring expects")
    void loginFormRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    @DisplayName("register form exposes every required field including password confirmation")
    void registerFormRenders() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(containsString("name=\"email\"")));
    }

    @Test
    @DisplayName("courses catalog lists a published course card")
    void coursesPageRenders() throws Exception {
        seedPublishedCourse("full-stack-web", "Full Stack Web Development");
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Full Stack Web Development")));
    }

    @Test
    @DisplayName("course detail page renders for a published slug")
    void courseDetailRenders() throws Exception {
        seedPublishedCourse("cloud-practitioner", "Cloud Practitioner");
        mockMvc.perform(get("/courses/cloud-practitioner"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cloud Practitioner")));
    }

    @Test
    @DisplayName("the student dashboard renders for a logged-in student")
    void studentDashboardRenders() throws Exception {
        seedStudent("ui-student@test.com", "password123");
        MockHttpSession session = login("ui-student@test.com", "password123");
        mockMvc.perform(get("/student/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("student dashboard surfaces the Future Mentor, a live-cohort course card and a working Join Class button")
    void studentDashboardLiveCohort() throws Exception {
        Course course = seedPublishedCourse("java-cohort", "Java Full Stack Cohort");
        User trainerUser = seedTrainer("live-trainer@test.com", "password123");
        Trainer trainer = trainerRepository.findByUser_Id(trainerUser.getId()).orElseThrow();
        Batch batch = batchRepository.save(Batch.builder()
                .batchName("Java Cohort A").course(course).trainer(trainer)
                .startDate(LocalDate.now()).mode(TrainingMode.ONLINE).published(true).maxSeats(50).build());

        User studentUser = seedStudent("live-student@test.com", "password123");
        Student s = studentRepository.findByUser_Id(studentUser.getId()).orElseThrow();
        enrollmentRepository.save(Enrollment.builder().student(s).course(course).batch(batch)
                .status(EnrollmentStatus.ACTIVE).trainingMode(TrainingMode.ONLINE).build());

        liveClassRepository.save(LiveClass.builder().batch(batch)
                .topic("Spring Dependency Injection")
                .startTime(LocalDateTime.now().plusHours(2)).endTime(LocalDateTime.now().plusHours(3))
                .mode(ClassMode.ONLINE).meetingLink("https://meet.example/jca")
                .status(ClassStatus.SCHEDULED).build());

        MockHttpSession session = login("live-student@test.com", "password123");
        mockMvc.perform(get("/student/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Future Mentor")))
                .andExpect(content().string(containsString("Continue Learning")))
                .andExpect(content().string(containsString("Live Classes")))
                .andExpect(content().string(containsString("Java Full Stack Cohort")))
                .andExpect(content().string(containsString("Join Class")))
                .andExpect(content().string(containsString("Spring Dependency Injection")));
    }

    @Test
    @DisplayName("the Future Mentor chat page renders for a logged-in student with disclaimer + suggested questions")
    void mentorChatPageRenders() throws Exception {
        seedStudent("mentor-ui@test.com", "password123");
        MockHttpSession session = login("mentor-ui@test.com", "password123");
        mockMvc.perform(get("/student/mentor").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Future Mentor")))
                .andExpect(content().string(containsString("can be wrong")))
                .andExpect(content().string(containsString("mentor-chip")))
                .andExpect(content().string(containsString("id=\"mentorForm\"")));
    }

    @Test
    @DisplayName("proceeding to payment shows a demo UPI QR page when the gateway is disabled")
    void checkoutShowsDemoQrInDemoMode() throws Exception {
        seedPublishedCourse("qr-checkout-course", "QR Checkout Course");
        seedStudent("checkout@test.com", "password123");
        MockHttpSession session = login("checkout@test.com", "password123");
        // Checkout page now leads to a payment step rather than showing the QR directly.
        mockMvc.perform(get("/student/checkout").param("slug", "qr-checkout-course").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proceed to Payment")));
        // After clicking through, the demo payment page renders the scannable QR.
        mockMvc.perform(withCsrf("/student/checkout/pay")
                        .param("slug", "qr-checkout-course").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scan to pay")))
                .andExpect(content().string(containsString("data:image/png;base64,")));
    }

    @Test
    @DisplayName("the admin dashboard renders for a logged-in admin")
    void adminDashboardRenders() throws Exception {
        seedUser("ui-admin@test.com", com.futureboundtech.enums.Role.ADMIN, "password123");
        MockHttpSession session = login("ui-admin@test.com", "password123");
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the public practice landing page renders (no longer a placeholder)")
    void practicePageRenders() throws Exception {
        mockMvc.perform(get("/practice"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Practice")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("currently under construction"))));
    }

    @Test
    @DisplayName("the public batches page lists an open published batch")
    void batchesPageRenders() throws Exception {
        Course course = seedPublishedCourse("java-boot", "Java Spring Boot");
        Trainer trainer = trainerRepository.findByUser_Id(seedTrainer("ui-trainer@test.com", "password123").getId())
                .orElseThrow();
        batchRepository.save(Batch.builder()
                .batchName("Java Boot — Jan Cohort").course(course).trainer(trainer)
                .startDate(java.time.LocalDate.now().plusDays(14))
                .mode(TrainingMode.ONLINE).published(true).maxSeats(30).build());
        mockMvc.perform(get("/batches"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Java Boot — Jan Cohort")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("currently under construction"))));
    }

    @Test
    @DisplayName("MCQ runner shows one question at a time with a position counter and a Next button")
    void mcqRunnerRendersWithCounterAndNext() throws Exception {
        seedMcq("Which collection keeps insertion order?", "B", "LinkedList preserves insertion order.");
        seedMcq("Which set is sorted?", "D", "TreeMap keeps keys in sorted order.");
        seedStudent("run-student@test.com", "password123");
        MockHttpSession session = login("run-student@test.com", "password123");
        mockMvc.perform(get("/student/practice/mcq/run").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Practice MCQ")))
                .andExpect(content().string(containsString("1 / 2")))
                .andExpect(content().string(containsString("Next")));
    }

    @Test
    @DisplayName("submitting an answer in the runner reveals the explanation and offers to advance")
    void mcqRunnerSubmitShowsExplanation() throws Exception {
        PracticeQuestion q = seedMcq("Which list is array-backed?", "A", "ArrayList is backed by an array.");
        seedStudent("run-submit@test.com", "password123");
        MockHttpSession session = login("run-submit@test.com", "password123");
        mockMvc.perform(withCsrf("/student/practice/mcq/run")
                        .param("questionId", String.valueOf(q.getId()))
                        .param("answer", "A")
                        .param("i", "0")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ArrayList is backed by an array.")));
    }

    @Test
    @DisplayName("coding problem page renders a self-solve workspace that persists the student's own solution")
    void codingWorkspaceRendersAndSaves() throws Exception {
        PracticeQuestion problem = seedCodingProblem("Write a method that sums 1..N.");
        seedStudent("code-student@test.com", "password123");
        MockHttpSession session = login("code-student@test.com", "password123");
        mockMvc.perform(get("/student/practice/coding/" + problem.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your solution")));
        mockMvc.perform(withCsrf("/student/practice/coding/" + problem.getId() + "/solution")
                        .param("language", "Java")
                        .param("code", "int sum(int n){ return n*(n+1)/2; }")
                        .param("solved", "true")
                        .session(session))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/practice/coding/" + problem.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("n*(n+1)/2")))
                .andExpect(content().string(containsString("Solved")));
    }
}
