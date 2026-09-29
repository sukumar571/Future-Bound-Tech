package com.futureboundtech.repository;

import com.futureboundtech.entity.Payment;
import com.futureboundtech.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByCourse_Id(Long courseId);

    long countByCoupon_Id(Long couponId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    List<Payment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<Payment> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    boolean existsByStudent_IdAndCourse_IdAndStatus(Long studentId, Long courseId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
