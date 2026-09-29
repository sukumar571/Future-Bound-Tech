package com.futureboundtech.db;

import com.futureboundtech.entity.*;
import com.futureboundtech.enums.*;
import com.futureboundtech.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database-layer tests run against the real schema Hibernate creates on H2:
 * relationships & cascades, unique constraints, enrollment capacity accounting,
 * payment-status persistence and the exact predicate the Phase 25 paid-gate uses.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseIntegrityTest {

    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TrainerRepository trainerRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired BatchRepository batchRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired CouponRepository couponRepository;

    private Course course(String slug) {
        return courseRepository.save(Course.builder()
                .title("Course " + slug).slug(slug).shortDescription("s").description("d")
                .category(CourseCategory.WEB_DEVELOPMENT).level(CourseLevel.BEGINNER)
                .durationInDays(30).fee(new BigDecimal("1000")).trainingMode(TrainingMode.ONLINE)
                .status(CourseStatus.PUBLISHED).publicListed(true).build());
    }

    private Student student(String email) {
        User u = userRepository.save(User.builder()
                .firstName("A").lastName("B").email(email).password("x").role(Role.STUDENT).build());
        Student s = Student.builder().user(u).build();
        return studentRepository.save(s);
    }

    private Trainer trainer(String email) {
        User u = userRepository.save(User.builder()
                .firstName("T").lastName("R").email(email).password("x").role(Role.TRAINER).build());
        return trainerRepository.save(Trainer.builder().user(u).expertise("java").build());
    }

    @Test
    @DisplayName("a unique email is enforced on users")
    void uniqueEmail() {
        student("dup@test.com");
        userRepository.flush();
        User second = User.builder().firstName("X").lastName("Y").email("dup@test.com")
                .password("x").role(Role.STUDENT).build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(second);
        });
    }

    @Test
    @DisplayName("a unique course slug is enforced")
    void uniqueSlug() {
        course("same-slug");
        assertThrows(DataIntegrityViolationException.class, () -> course("same-slug"));
    }

    @Test
    @DisplayName("a unique coupon code is enforced")
    void uniqueCouponCode() {
        couponRepository.save(Coupon.builder().code("SAVE").discountAmount(BigDecimal.TEN).build());
        couponRepository.flush();
        assertThrows(DataIntegrityViolationException.class, () ->
                couponRepository.saveAndFlush(Coupon.builder().code("SAVE").build()));
    }

    @Test
    @DisplayName("User -> Student -> Enrollment -> Course relationships are navigable")
    void relationships() {
        Course c = course("rel-course");
        Student s = student("rel@test.com");
        Enrollment e = enrollmentRepository.save(Enrollment.builder()
                .student(s).course(c).status(EnrollmentStatus.ACTIVE).build());

        Enrollment loaded = enrollmentRepository.findById(e.getId()).orElseThrow();
        assertEquals(c.getId(), loaded.getCourse().getId());
        assertEquals(s.getId(), loaded.getStudent().getId());
        assertEquals(s.getUser().getId(),
                studentRepository.findById(s.getId()).orElseThrow().getUser().getId());
    }

    @Test
    @DisplayName("countSeatsHeldByBatch counts PENDING/ACTIVE/COMPLETED but not CANCELLED")
    void enrollmentCapacity() {
        Course c = course("cap-course");
        Trainer t = trainer("cap-trainer@test.com");
        Batch batch = batchRepository.save(Batch.builder()
                .batchName("B1").course(c).trainer(t).maxSeats(2).published(true)
                .enrollmentStatus(BatchEnrollmentStatus.OPEN).mode(TrainingMode.ONLINE).build());

        enrollmentRepository.save(Enrollment.builder().student(student("c1@test.com")).course(c)
                .batch(batch).status(EnrollmentStatus.PENDING_PAYMENT).build());
        enrollmentRepository.save(Enrollment.builder().student(student("c2@test.com")).course(c)
                .batch(batch).status(EnrollmentStatus.ACTIVE).build());
        enrollmentRepository.save(Enrollment.builder().student(student("c3@test.com")).course(c)
                .batch(batch).status(EnrollmentStatus.CANCELLED).build());
        enrollmentRepository.flush();

        assertEquals(2, enrollmentRepository.countSeatsHeldByBatch(batch.getId()));
        assertEquals(3, enrollmentRepository.countByBatch_Id(batch.getId()));
    }

    @Test
    @DisplayName("payment status persists and only SUCCESS is treated as settled")
    void paymentStatuses() {
        Course c = course("pay-course");
        Student s = student("pay@test.com");
        Payment success = paymentRepository.save(Payment.builder()
                .student(s).course(c).receiptNumber("RCT-1").amount(new BigDecimal("500"))
                .status(PaymentStatus.SUCCESS).build());
        Payment created = paymentRepository.save(Payment.builder()
                .student(s).course(c).receiptNumber("RCT-2").amount(new BigDecimal("500"))
                .status(PaymentStatus.CREATED).build());
        paymentRepository.flush();

        assertTrue(paymentRepository.findById(success.getId()).orElseThrow().isSettled());
        assertFalse(paymentRepository.findById(created.getId()).orElseThrow().isSettled());
        assertEquals(0, paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)
                .compareTo(new BigDecimal("500")));
    }

    @Test
    @DisplayName("PHASE 25 paid-gate: a PENDING_PAYMENT seat grants no content; ACTIVE does")
    void paidGatePredicate() {
        Course c = course("gate-course");
        Student s = student("gate@test.com");
        enrollmentRepository.save(Enrollment.builder().student(s).course(c)
                .status(EnrollmentStatus.PENDING_PAYMENT).build());
        enrollmentRepository.flush();

        List<EnrollmentStatus> paid = List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);
        assertFalse(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(s.getId(), c.getId(), paid),
                "an unpaid seat must not satisfy the paid predicate");
        assertTrue(enrollmentRepository.existsByStudent_IdAndCourse_Id(s.getId(), c.getId()),
                "but the enrollment row itself exists");

        Optional<Enrollment> found = enrollmentRepository.findByStudent_IdAndCourse_Id(s.getId(), c.getId());
        Enrollment e = found.orElseThrow();
        e.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.saveAndFlush(e);

        assertTrue(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(s.getId(), c.getId(), paid),
                "once paid, the predicate unlocks content");
    }
}
