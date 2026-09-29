package com.futureboundtech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A placement / job opportunity post (Phase 17). Admin-curated hiring posts with
 * eligibility, skills, deadline and an authorized external application link.
 * Nothing here is executed internally: applications always go to the external
 * link supplied by the admin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "placements")
public class Placement extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false)
    private String companyName;

    @NotBlank
    @Size(max = 150)
    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    /** Comma-separated skill keywords, e.g. "Java, Spring Boot, SQL". */
    @Size(max = 500)
    private String skills;

    @Size(max = 120)
    private String location;

    @Size(max = 60)
    private String jobType;

    @Column(name = "deadline_date")
    private LocalDate deadline;

    /** Authorized external application URL (http/https only, validated in the service). */
    @Size(max = 500)
    @Column(name = "application_url")
    private String applicationUrl;

    @Column(name = "prep_resources", columnDefinition = "TEXT")
    private String prepResources;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;
}
