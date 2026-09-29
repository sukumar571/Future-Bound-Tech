import os

base_pkg = "com.futureboundtech"
base_dir = f"src/main/java/{base_pkg.replace('.', '/')}"

dirs = ["entity", "repository", "enums", "dto"]
for d in dirs:
    os.makedirs(os.path.join(base_dir, d), exist_ok=True)

def write_file(sub_dir, file_name, content):
    path = os.path.join(base_dir, sub_dir, file_name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

# ENUMS
enums = {
    "CourseLevel": "BEGINNER, INTERMEDIATE, ADVANCED",
    "TrainingMode": "ONLINE, OFFLINE, HYBRID",
    "EnrollmentStatus": "ACTIVE, COMPLETED, DROPPED",
    "PaymentStatus": "PENDING, SUCCESS, FAILED, REFUNDED",
    "SubmissionStatus": "PENDING, GRADED, REJECTED",
    "ClassStatus": "SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED",
    "NotificationType": "INFO, WARNING, ALERT, MESSAGE"
}

for e_name, e_vals in enums.items():
    content = f"""package {base_pkg}.enums;

public enum {e_name} {{
    {e_vals}
}}
"""
    write_file("enums", f"{e_name}.java", content)


# BASE ENTITY
base_entity = f"""package {base_pkg}.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}}
"""
    write_file("entity", "BaseEntity.java", base_entity)


# ENTITIES TEMPLATES
entities_data = [
    {
        "name": "User",
        "imports": ["com.futureboundtech.enums.Role", "java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Student studentProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Trainer trainerProfile;
    
    private boolean active = true;
"""
    },
    {
        "name": "Student",
        "imports": ["java.util.List", "java.util.ArrayList", "com.fasterxml.jackson.annotation.JsonIgnore"],
        "fields": f"""
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String education;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
"""
    },
    {
        "name": "Trainer",
        "imports": ["java.util.List", "java.util.ArrayList", "com.fasterxml.jackson.annotation.JsonIgnore"],
        "fields": f"""
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String expertise;
    
    @Column(length = 1000)
    private String bio;
    
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL)
    private List<Course> courses = new ArrayList<>();
    
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL)
    private List<Batch> batches = new ArrayList<>();
"""
    },
    {
        "name": "Course",
        "imports": ["java.math.BigDecimal", "com.futureboundtech.enums.CourseLevel", "com.futureboundtech.enums.TrainingMode", "java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal fee;
    
    @Column(nullable = false)
    private Integer durationInDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingMode trainingMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseModule> modules = new ArrayList<>();
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Batch> batches = new ArrayList<>();
"""
    },
    {
        "name": "CourseModule",
        "imports": ["java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String title;
    
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();
"""
    },
    {
        "name": "Lesson",
        "imports": ["java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String content;

    private String videoUrl;
    
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonResource> resources = new ArrayList<>();
"""
    },
    {
        "name": "LessonResource",
        "imports": [],
        "fields": f"""
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String fileUrl;
    
    private String resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
"""
    },
    {
        "name": "Batch",
        "imports": ["com.futureboundtech.enums.TrainingMode", "com.futureboundtech.enums.ClassStatus", "java.util.List", "java.util.ArrayList", "java.time.LocalDate", "java.time.LocalTime"],
        "fields": f"""
    @Column(nullable = false)
    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    private LocalDate startDate;
    private LocalDate endDate;
    
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private TrainingMode mode;
    
    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.SCHEDULED;

    private Integer maxSeats;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<LiveClass> liveClasses = new ArrayList<>();
"""
    },
    {
        "name": "Enrollment",
        "imports": ["com.futureboundtech.enums.EnrollmentStatus"],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
    
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL)
    private Certificate certificate;
"""
    },
    {
        "name": "Payment",
        "imports": ["java.math.BigDecimal", "com.futureboundtech.enums.PaymentStatus"],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
"""
    },
    {
        "name": "PaymentOrder",
        "imports": ["java.math.BigDecimal"],
        "fields": f"""
    @Column(nullable = false, unique = true)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private BigDecimal amount;

    private String status;
"""
    },
    {
        "name": "Coupon",
        "imports": ["java.math.BigDecimal", "java.time.LocalDateTime"],
        "fields": f"""
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private BigDecimal discountAmount;

    private LocalDateTime validUntil;

    private boolean isActive = true;
"""
    },
    {
        "name": "LiveClass",
        "imports": ["com.futureboundtech.enums.ClassStatus", "java.time.LocalDateTime", "java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String meetingLink;

    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.SCHEDULED;
    
    @OneToMany(mappedBy = "liveClass", cascade = CascadeType.ALL)
    private List<Attendance> attendances = new ArrayList<>();
"""
    },
    {
        "name": "Attendance",
        "imports": [],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_class_id", nullable = false)
    private LiveClass liveClass;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    private boolean isPresent = false;
"""
    },
    {
        "name": "Assignment",
        "imports": ["java.time.LocalDateTime", "java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    private LocalDateTime dueDate;
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    private List<Submission> submissions = new ArrayList<>();
"""
    },
    {
        "name": "Submission",
        "imports": ["com.futureboundtech.enums.SubmissionStatus"],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String fileUrl;
    
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status = SubmissionStatus.PENDING;
    
    private Integer score;
    private String feedback;
"""
    },
    {
        "name": "Quiz",
        "imports": ["java.util.List", "java.util.ArrayList"],
        "fields": f"""
    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer timeLimitMinutes;
    private Integer passingScore;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuizQuestion> questions = new ArrayList<>();
"""
    },
    {
        "name": "QuizQuestion",
        "imports": [],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, length = 1000)
    private String questionText;
    
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    
    @Column(nullable = false)
    private String correctOption;
"""
    },
    {
        "name": "QuizAttempt",
        "imports": [],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Integer scoreObtained;
    private boolean isPassed;
"""
    },
    {
        "name": "Certificate",
        "imports": ["java.time.LocalDate"],
        "fields": f"""
    @Column(unique = true, nullable = false)
    private String certificateNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    private LocalDate issueDate;
    private String certificateUrl;
"""
    },
    {
        "name": "Announcement",
        "imports": ["java.time.LocalDateTime"],
        "fields": f"""
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch; // null means global announcement
"""
    },
    {
        "name": "Notification",
        "imports": ["com.futureboundtech.enums.NotificationType"],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type = NotificationType.INFO;
    
    private boolean isRead = false;
"""
    },
    {
        "name": "Review",
        "imports": [],
        "fields": f"""
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private Integer rating;
    
    @Column(length = 1000)
    private String comment;
    
    private boolean isApproved = false;
"""
    },
    {
        "name": "ContactMessage",
        "imports": [],
        "fields": f"""
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(nullable = false, length = 2000)
    private String message;
    
    private boolean isReplied = false;
"""
    },
    {
        "name": "InstituteSettings",
        "imports": ["java.math.BigDecimal"],
        "fields": f"""
    @Column(nullable = false)
    private String name;
    
    private String tagLine;
    private String phone1;
    private String phone2;
    private String email;
    
    @Column(length = 1000)
    private String address;

    private BigDecimal defaultRegistrationFee;
"""
    },
    {
        "name": "AuditLog",
        "imports": [],
        "fields": f"""
    private String action;
    
    @Column(nullable = false)
    private String username;
    
    @Column(length = 2000)
    private String details;
    
    private String ipAddress;
"""
    }
]

for entity in entities_data:
    name = entity["name"]
    imports = "\n".join([f"import {pkg};" for pkg in entity.get("imports", [])])
    fields = entity["fields"]
    
    content = f"""package {base_pkg}.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
{imports}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "{name.lower()}s")
public class {name} extends BaseEntity {{
{fields}
}}
"""
    write_file("entity", f"{name}.java", content)


# CREATE REPOSITORIES
for entity in entities_data:
    name = entity["name"]
    content = f"""package {base_pkg}.repository;

import {base_pkg}.entity.{name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {name}Repository extends JpaRepository<{name}, Long> {{
}}
"""
    write_file("repository", f"{name}Repository.java", content)
    
print("Successfully generated all Enums, Entities, and Repositories!")
