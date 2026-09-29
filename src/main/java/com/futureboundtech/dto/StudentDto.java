package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Read model for the admin Students table and student detail page. */
@Data
@Accessors(chain = true)
public class StudentDto {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String education;
    private boolean active;
    private long enrolledCourses;
    private long completedCourses;
    private String paymentSummary;
    private LocalDateTime createdAt;
}
