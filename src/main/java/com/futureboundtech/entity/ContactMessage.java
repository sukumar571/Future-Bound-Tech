package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contactmessages")
public class ContactMessage extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @Email
    @NotBlank
    @Column(nullable = false)
    private String email;
    
    @Size(max = 40)
    @Column(length = 40)
    private String phone;

    @Size(max = 160)
    @Column(name = "course_interest", length = 160)
    private String courseInterest;

    @NotBlank
    @Column(nullable = false)
    private String subject;
    
    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String message;
    
    private boolean isReplied = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.futureboundtech.enums.ContactStatus status = com.futureboundtech.enums.ContactStatus.NEW;

}
