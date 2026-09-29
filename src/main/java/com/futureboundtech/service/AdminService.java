package com.futureboundtech.service;

import com.futureboundtech.dto.*;
import com.futureboundtech.enums.BatchEnrollmentStatus;
import com.futureboundtech.enums.ContactStatus;
import com.futureboundtech.enums.EnrollmentStatus;

import java.util.List;

/**
 * Aggregation and management operations for the admin portal.
 * Every read is scoped to the whole institute (ADMIN only) and served from MySQL.
 */
public interface AdminService {

    AdminStatsDto dashboardStats();

    // ---- Students -------------------------------------------------------
    List<StudentDto> listStudents(String query, Boolean active);

    StudentDto getStudent(Long studentId);

    List<EnrollmentDto> studentEnrollments(Long studentId);

    List<PaymentDto> studentPayments(Long studentId);

    void createStudent(StudentFormDto form);

    void updateStudent(Long studentId, StudentFormDto form);

    void setStudentActive(Long studentId, boolean active);

    // ---- Trainers -------------------------------------------------------
    List<TrainerDto> listTrainers(String query, Boolean active);

    TrainerDto getTrainer(Long trainerId);

    void createTrainer(TrainerFormDto form);

    void updateTrainer(Long trainerId, TrainerFormDto form);

    void setTrainerActive(Long trainerId, boolean active);

    // ---- Batches --------------------------------------------------------
    List<BatchDto> listBatches();

    BatchDto getBatch(Long batchId);

    void createBatch(BatchDto dto);

    void updateBatch(Long batchId, BatchDto dto);

    void deleteBatch(Long batchId);

    void setBatchPublished(Long batchId, boolean published);

    void setBatchEnrollmentStatus(Long batchId, BatchEnrollmentStatus status);

    // ---- Live classes ---------------------------------------------------
    List<LiveClassDto> listLiveClasses();

    LiveClassDto getLiveClass(Long id);

    void createLiveClass(LiveClassDto dto);

    void updateLiveClass(Long id, LiveClassDto dto);

    void deleteLiveClass(Long id);

    // ---- Enrollments ----------------------------------------------------
    List<EnrollmentDto> listEnrollments(String query, EnrollmentStatus status);

    void setEnrollmentStatus(Long enrollmentId, EnrollmentStatus status);

    // ---- Coupons --------------------------------------------------------
    List<CouponDto> listCoupons();

    /** Lightweight id/title pairs used to populate coupon course-restriction pickers. */
    List<NameValueDto> listCourseOptions();

    CouponDto getCoupon(Long id);

    void createCoupon(CouponDto dto);

    void updateCoupon(Long id, CouponDto dto);

    void deleteCoupon(Long id);

    // ---- Announcements --------------------------------------------------
    List<AnnouncementDto> listAnnouncements();

    AnnouncementDto getAnnouncement(Long id);

    void createAnnouncement(AnnouncementDto dto);

    void updateAnnouncement(Long id, AnnouncementDto dto);

    void deleteAnnouncement(Long id);

    // ---- Contact messages ----------------------------------------------
    List<ContactMessageDto> listContactMessages();

    List<ContactMessageDto> listContactMessages(String query, ContactStatus status);

    void setContactReplied(Long id, boolean replied);

    void setContactStatus(Long id, ContactStatus status);

    // ---- Certificates ---------------------------------------------------
    List<CertificateDto> listCertificates();

    // ---- Reviews / testimonials ----------------------------------------
    List<ReviewDto> listReviews();

    ReviewDto getReview(Long id);

    void setReviewApproved(Long id, boolean approved);

    void createReview(ReviewDto dto);

    void updateReview(Long id, ReviewDto dto);

    void deleteReview(Long id);

    void setReviewPublished(Long id, boolean published);

    // ---- FAQs -----------------------------------------------------------
    List<FaqDto> listFaqs();

    FaqDto getFaq(Long id);

    void createFaq(FaqDto dto);

    void updateFaq(Long id, FaqDto dto);

    void deleteFaq(Long id);

    void setFaqPublished(Long id, boolean published);

    // ---- Assignments & quizzes (read) ----------------------------------
    List<AssignmentDto> listAssignments();

    List<QuizDto> listQuizzes();

    // ---- Settings -------------------------------------------------------
    InstituteSettingsDto getSettings();

    void saveSettings(InstituteSettingsDto dto);
}
