package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read model for a placement post (Phase 17), used by public, student and admin views. */
@Data
@Accessors(chain = true)
public class PlacementDto {
    private Long id;
    private String companyName;
    private String jobTitle;
    private String description;
    private String eligibility;
    /** Raw comma-separated skills. */
    private String skills;
    /** Parsed skill chips. */
    private List<String> skillList = new ArrayList<>();
    private String location;
    private String jobType;
    private LocalDate deadline;
    private String applicationUrl;
    private String prepResources;
    private boolean published;
    /** Whether the viewing student has bookmarked this opportunity (null when not applicable). */
    private Boolean saved;

    /** True when the application deadline date has passed. */
    public boolean isDeadlinePassed() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    /** True when the deadline falls within the next 7 days (and hasn't passed). */
    public boolean isDeadlineClosingSoon() {
        return deadline != null && !isDeadlinePassed() && !deadline.isAfter(LocalDate.now().plusDays(7));
    }
}
