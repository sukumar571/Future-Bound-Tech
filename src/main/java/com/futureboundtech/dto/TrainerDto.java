package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class TrainerDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String expertise;
    private String email;
    private String phone;
    private String bio;
    private Integer experienceYears;
    private String photoUrl;
    private boolean active;
    private long courseCount;
    private long batchCount;
    private LocalDateTime createdAt;
}
