package com.futureboundtech.dto;

import com.futureboundtech.enums.EnrollmentStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** A single row in the student's enrollment history. */
@Data
@Accessors(chain = true)
public class StudentEnrollmentDto {

    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private String thumbnailUrl;

    private String batchName;
    private String trainingModeLabel;

    private String amountLabel;
    private String paymentStatusLabel;

    private EnrollmentStatus status;
    private String statusLabel;

    private int completionPercent;
    private LocalDateTime enrolledAt;

    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == EnrollmentStatus.COMPLETED;
    }

    public boolean isPendingPayment() {
        return status == EnrollmentStatus.PENDING_PAYMENT;
    }
}
