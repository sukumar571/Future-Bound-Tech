package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentPaymentDto {

    private Long id;
    private String courseTitle;
    private Long courseId;
    private String receiptNumber;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private String currency;
    private String status;        // CREATED / PENDING / SUCCESS / FAILED / REFUNDED / CANCELLED
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
