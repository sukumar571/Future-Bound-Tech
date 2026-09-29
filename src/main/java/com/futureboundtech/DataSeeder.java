package com.futureboundtech;

import com.futureboundtech.entity.Announcement;
import com.futureboundtech.entity.Batch;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.repository.AnnouncementRepository;
import com.futureboundtech.repository.BatchRepository;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.TrainerRepository;
import com.futureboundtech.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeders.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    /** Bootstrap admin credential. Override with ADMIN_SEED_PASSWORD in production. */
    @org.springframework.beans.factory.annotation.Value("${app.seed.admin-password:password}")
    private String adminSeedPassword;

    @org.springframework.beans.factory.annotation.Value("${app.seed.admin-email:mine@gmail.com}")
    private String adminSeedEmail;

    /** The original catch-all trainer; courses still pointing here get a specialist. */
    private static final String GENERIC_TRAINER_EMAIL = "trainer@futureboundtech.com";

    /** A trainer profile to seed. Idempotent — matched on e-mail. */
    private record TrainerSpec(String firstName, String lastName, String email, String phone,
                               String expertise, String bio) {}

    /** Demo course pricing + the specialist trainer index assigned to each catalog course. */
    private record CommerceSpec(String slug, BigDecimal fee, BigDecimal discountFee, int trainerIndex) {}

    private static final List<TrainerSpec> TRAINER_SPECS = List.of(
            new TrainerSpec("Priya", "Sharma", "trainer@futureboundtech.com", "9000000001",
                    "Java Full Stack and Spring Boot",
                    "Senior full-stack trainer at Future Bound Tech leading the Java program, from core Java through Spring Boot and capstone projects."),
            new TrainerSpec("Rahul", "Verma", "rahul.verma@futureboundtech.com", "9000000002",
                    "Python, Django and Flask",
                    "Backend specialist mentoring the Python Full Stack track — Django, Flask, REST APIs and production project structure."),
            new TrainerSpec("Neha", "Gupta", "neha.gupta@futureboundtech.com", "9000000003",
                    "HTML, CSS, JavaScript and responsive UI",
                    "Frontend coach for the Web Development track, focused on semantic HTML, modern CSS, JavaScript and accessible interfaces."),
            new TrainerSpec("Arjun", "Nair", "arjun.nair@futureboundtech.com", "9000000004",
                    "AWS cloud, networking and DevOps",
                    "Cloud engineer running the AWS Computing track — IAM, EC2, S3, VPC, serverless and deployment workflows."),
            new TrainerSpec("Kavya", "Iyer", "kavya.iyer@futureboundtech.com", "9000000005",
                    "Machine learning, deep learning and NLP",
                    "Data scientist leading the Artificial Intelligence track, from data handling to model evaluation and applied projects.")
    );

    /** Uniform demo price applied to every catalog course (admin can still override). */
    private static final BigDecimal DEMO_FEE = new BigDecimal("29999");

    /**
     * Legacy demo fees previously written by the seeder. A course still carrying one of
     * these values (or no fee at all) is treated as demo data and re-priced to DEMO_FEE;
     * any other price is assumed to be an explicit admin choice and left untouched.
     */
    private static final List<BigDecimal> LEGACY_DEMO_FEES = List.of(
            new BigDecimal("45000"), new BigDecimal("40000"), new BigDecimal("25000"),
            new BigDecimal("35000"), new BigDecimal("50000"));

    private static boolean isDemoFee(BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        for (BigDecimal legacy : LEGACY_DEMO_FEES) {
            if (legacy.compareTo(fee) == 0) {
                return true;
            }
        }
        return false;
    }

    private static final List<CommerceSpec> COMMERCE = List.of(
            new CommerceSpec("java-full-stack-development", DEMO_FEE, null, 0),
            new CommerceSpec("python-full-stack-development", DEMO_FEE, null, 1),
            new CommerceSpec("web-development", DEMO_FEE, null, 2),
            new CommerceSpec("aws-cloud-computing", DEMO_FEE, null, 3),
            new CommerceSpec("artificial-intelligence", DEMO_FEE, null, 4)
    );

    @Bean
    @Order(1)
    public CommandLineRunner initData() {
        return args -> {
            // Run the whole seed in one transaction so lazy associations (e.g. a course's
            // trainer) stay initialisable while the catalog is backfilled.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                User admin = seedAdmin();
                List<Trainer> trainers = seedTrainers();
                seedCourses(trainers);
                backfillCatalog(trainers);
                seedAnnouncements();
                if (admin != null) {
                    // Never log credentials — the seeded e-mail is fine, the password is not.
                    log.info("Default admin user created ({}). Set a new password immediately.",
                            admin.getEmail());
                }
            });
        };
    }

    /**
     * Demo content for the announcements page. Deliberately written straight to the
     * repository: historical notices are displayed, not re-broadcast into inboxes.
     */
    private void seedAnnouncements() {
        if (announcementRepository.count() > 0) {
            return;
        }
        announcementRepository.save(Announcement.builder()
                .title("Welcome to Future Bound Tech")
                .content("New batch timetables are published every Monday. Check the Announcements page "
                        + "and your notification inbox for changes to schedules and deadlines.")
                .build());
        Batch batch = batchRepository.findAllByOrderByCreatedAtDesc().stream().findFirst().orElse(null);
        if (batch != null) {
            announcementRepository.save(Announcement.builder()
                    .title("Session reminder for " + batch.getBatchName())
                    .content("Please join the live class five minutes early and keep your assignment "
                            + "submissions coming — they count towards your certificate.")
                    .batch(batch)
                    .build());
        }
    }

    private User seedAdmin() {
        if (userRepository.findByEmail(adminSeedEmail).isPresent()) {
            return null;
        }
        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminSeedEmail)
                .password(passwordEncoder.encode(adminSeedPassword))
                .phone("8978866005")
                .role(Role.ADMIN)
                .active(true)
                .build();
        return userRepository.save(admin);
    }

    /** Ensures the five specialist trainers exist (idempotent, matched on e-mail). */
    private List<Trainer> seedTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        for (TrainerSpec spec : TRAINER_SPECS) {
            trainers.add(seedTrainer(spec));
        }
        return trainers;
    }

    private Trainer seedTrainer(TrainerSpec spec) {
        User existing = userRepository.findByEmail(spec.email()).orElse(null);
        if (existing == null) {
            User user = User.builder()
                    .firstName(spec.firstName())
                    .lastName(spec.lastName())
                    .email(spec.email())
                    .password(passwordEncoder.encode("password"))
                    .phone(spec.phone())
                    .role(Role.TRAINER)
                    .active(true)
                    .build();
            Trainer trainer = Trainer.builder()
                    .user(user)
                    .expertise(spec.expertise())
                    .bio(spec.bio())
                    .build();
            user.setTrainerProfile(trainer);
            userRepository.save(user);
            return trainerRepository.findByUser_Id(user.getId()).orElse(trainer);
        }
        return trainerRepository.findByUser_Id(existing.getId()).orElseGet(() -> {
            Trainer created = Trainer.builder()
                    .user(existing)
                    .expertise(spec.expertise())
                    .bio(spec.bio())
                    .build();
            existing.setTrainerProfile(created);
            userRepository.save(existing);
            return trainerRepository.findByUser_Id(existing.getId()).orElse(created);
        });
    }

    private void seedCourses(List<Trainer> trainers) {
        if (courseRepository.count() > 0) {
            return;
        }

        saveCourse(
                "Java Full Stack Development",
                "java-full-stack-development",
                "Build enterprise-grade applications with Java, Spring Boot, Hibernate, REST APIs, and modern frontend technologies.",
                "This program covers Core Java through Spring Boot, persistence, REST APIs, and a coordinated frontend so learners can ship full-stack features. Fees are indicative and can be revised by the institute in the admin course editor.",
                CourseCategory.FULL_STACK,
                CourseLevel.BEGINNER,
                4,
                """
                        Write Java applications using OOP, collections, and exception handling
                        Build REST APIs with Spring Boot
                        Map data with JPA/Hibernate and MySQL
                        Connect a frontend to backend services
                        Deploy a complete full-stack project
                        """,
                """
                        Basic computer literacy
                        Willingness to practice coding daily
                        No prior Java experience required
                        """,
                """
                        Student management REST API
                        Institute portal with Spring Boot and Thymeleaf
                        Capstone full-stack application
                        """,
                0,
                trainers
        );

        saveCourse(
                "Python Full Stack Development",
                "python-full-stack-development",
                "Learn Python, Django, Flask, databases, and modern web development to build powerful full-stack applications.",
                "Learners work through Python fundamentals, web frameworks, relational data, and project structure used in production teams. Fees are indicative and can be revised by the institute in the admin course editor.",
                CourseCategory.FULL_STACK,
                CourseLevel.BEGINNER,
                4,
                """
                        Write clean Python for real applications
                        Build web apps with Django and Flask
                        Design relational schemas and queries
                        Expose and consume APIs
                        Complete a full-stack Python project
                        """,
                """
                        Basic programming interest
                        Familiarity with using a code editor
                        """,
                """
                        Blog or CMS with Django
                        REST service with Flask
                        Capstone product with authentication
                        """,
                1,
                trainers
        );

        saveCourse(
                "Web Development",
                "web-development",
                "Master HTML5, CSS3, JavaScript, Bootstrap, and responsive design to create modern websites.",
                "A frontend-focused path covering semantic HTML, modern CSS, JavaScript, and Bootstrap layouts used across Future Bound Tech products. Fees are indicative and can be revised by the institute in the admin course editor.",
                CourseCategory.WEB_DEVELOPMENT,
                CourseLevel.INTERMEDIATE,
                3,
                """
                        Structure pages with semantic HTML5
                        Style responsive layouts with CSS3 and Bootstrap
                        Add interactivity with JavaScript
                        Build reusable UI components
                        Publish a multi-page marketing site
                        """,
                """
                        Comfortable using a browser and text editor
                        Helpful: any prior exposure to HTML
                        """,
                """
                        Personal portfolio site
                        Landing page with Bootstrap components
                        Interactive UI mini-project
                        """,
                2,
                trainers
        );

        saveCourse(
                "AWS Cloud Computing",
                "aws-cloud-computing",
                "Deploy and manage scalable cloud infrastructure with Amazon Web Services and prepare for cloud operations work.",
                "The syllabus introduces core AWS services used to host applications: compute, storage, databases, and serverless building blocks. Certification-oriented practice is included; fees are indicative and configurable in admin.",
                CourseCategory.CLOUD_COMPUTING,
                CourseLevel.ADVANCED,
                3,
                """
                        Navigate the AWS console and IAM basics
                        Launch compute and storage services
                        Use managed databases and serverless functions
                        Apply networking and security fundamentals
                        Document a cloud deployment
                        """,
                """
                        Understanding of application hosting basics
                        Preferred: prior programming or sysadmin exposure
                        """,
                """
                        Static site on object storage
                        Virtual server web deployment
                        Serverless API prototype
                        """,
                3,
                trainers
        );

        saveCourse(
                "Artificial Intelligence",
                "artificial-intelligence",
                "Explore machine learning, deep learning, NLP, and computer vision. Build intelligent systems with guided projects.",
                "Students progress from data handling into classical ML, then selected deep learning, NLP, and vision workflows. Fees are indicative and can be revised by institute staff in the admin course editor.",
                CourseCategory.ARTIFICIAL_INTELLIGENCE,
                CourseLevel.ADVANCED,
                5,
                """
                        Prepare datasets for model training
                        Train and evaluate classical ML models
                        Apply introductory deep learning workflows
                        Experiment with NLP or vision tasks
                        Present an AI project with clear metrics
                        """,
                """
                        Python programming fundamentals
                        High-school mathematics (algebra, probability)
                        """,
                """
                        Tabular prediction project
                        Text classification or chatbot prototype
                        Capstone AI use-case with evaluation report
                        """,
                4,
                trainers
        );
    }

    /**
     * Idempotent upgrade for an existing catalog: re-prices unpublished or legacy-demo
     * fees to the current demo price and moves any course still on the generic catch-all
     * trainer to its specialist. Genuinely admin-set prices are never overwritten.
     */
    private void backfillCatalog(List<Trainer> trainers) {
        for (CommerceSpec spec : COMMERCE) {
            courseRepository.findBySlugAndDeletedFalse(spec.slug()).ifPresent(course -> {
                boolean changed = false;
                if (isDemoFee(course.getFee())) {
                    if (course.getFee() == null || spec.fee().compareTo(course.getFee()) != 0) {
                        course.setFee(spec.fee());
                        changed = true;
                    }
                    // Demo courses carry no strike-through discount.
                    if (course.getDiscountFee() != null) {
                        course.setDiscountFee(null);
                        changed = true;
                    }
                }
                Trainer target = trainers.get(spec.trainerIndex());
                Trainer current = course.getTrainer();
                boolean needsTrainer = current == null
                        || (current.getUser() != null
                            && GENERIC_TRAINER_EMAIL.equals(current.getUser().getEmail())
                            && !current.getId().equals(target.getId()));
                if (needsTrainer) {
                    course.setTrainer(target);
                    changed = true;
                }
                if (changed) {
                    courseRepository.save(course);
                }
            });
        }
    }

    private void saveCourse(String title,
                            String slug,
                            String shortDescription,
                            String detailedDescription,
                            CourseCategory category,
                            CourseLevel level,
                            int durationMonths,
                            String outcomes,
                            String prerequisites,
                            String projects,
                            int trainerIndex,
                            List<Trainer> trainers) {
        CommerceSpec commerce = COMMERCE.stream()
                .filter(c -> c.slug().equals(slug)).findFirst().orElse(null);
        Course course = Course.builder()
                .title(title)
                .slug(slug)
                .shortDescription(shortDescription)
                .description(detailedDescription)
                .category(category)
                .level(level)
                .durationInDays(durationMonths * 30)
                .fee(commerce != null ? commerce.fee() : BigDecimal.ZERO)
                .discountFee(commerce != null ? commerce.discountFee() : null)
                .trainingMode(TrainingMode.HYBRID)
                .trainer(trainers.get(trainerIndex))
                .status(CourseStatus.PUBLISHED)
                .publicListed(true)
                .certificateEligible(true)
                .deleted(false)
                .learningOutcomes(outcomes.trim())
                .prerequisites(prerequisites.trim())
                .projects(projects.trim())
                .build();
        courseRepository.save(course);
    }
}
