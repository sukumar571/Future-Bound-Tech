package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentProfileDto {

    private Long studentId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String initials;
    private String email;
    private String phone;
    private String education;
    private String avatarUrl;
    private LocalDateTime memberSince;
}
