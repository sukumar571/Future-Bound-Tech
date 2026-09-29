package com.futureboundtech.dto;

import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.enums.TrainingMode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Enrollment read model for the admin Enrollments table. */
@Data
@Accessors(chain = true)
public class EnrollmentDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long courseId;
    private String courseTitle;
    private Long batchId;
    private String batchName;

    private TrainingMode trainingMode;
    private String trainingModeLabel;

    private BigDecimal amount;
    private String amountLabel;

    private PaymentStatus paymentStatus;
    private String paymentStatusLabel;

    private EnrollmentStatus status;
    private String statusLabel;

    private int completionPercent;
    private LocalDateTime enrolledAt;
}
