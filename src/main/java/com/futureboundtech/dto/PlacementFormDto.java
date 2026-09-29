package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Admin create/edit form backing object for placement posts (Phase 17). */
@Data
@Accessors(chain = true)
public class PlacementFormDto {
    private Long id;

    @NotBlank(message = "Company name is required")
    @Size(max = 150)
    private String companyName;

    @NotBlank(message = "Job title is required")
    @Size(max = 150)
    private String jobTitle;

    @Size(max = 4000)
    private String description;

    @Size(max = 2000)
    private String eligibility;

    @Size(max = 500)
    private String skills;

    @Size(max = 120)
    private String location;

    @Size(max = 60)
    private String jobType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    @Size(max = 500)
    private String applicationUrl;

    @Size(max = 4000)
    private String prepResources;

    private boolean published;
}
