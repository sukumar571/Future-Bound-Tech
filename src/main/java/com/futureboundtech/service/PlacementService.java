package com.futureboundtech.service;

import com.futureboundtech.dto.PlacementDto;
import com.futureboundtech.dto.PlacementFormDto;
import com.futureboundtech.entity.User;

import java.util.List;

/**
 * Placement preparation module (Phase 17): admin-curated job posts,
 * public/student browsing, skill filtering and saved opportunities.
 */
public interface PlacementService {

    // ------------------------------------------------------------- admin
    List<PlacementDto> listAll();

    PlacementDto getPlacement(Long id);

    PlacementFormDto getPlacementForm(Long id);

    PlacementDto createPlacement(PlacementFormDto form);

    PlacementDto updatePlacement(Long id, PlacementFormDto form);

    void deletePlacement(Long id);

    void setPublished(Long id, boolean published);

    // ------------------------------------------------------------- public / student
    /** Published posts, newest first, optionally filtered by a skill keyword. */
    List<PlacementDto> listPublished(String skill);

    /** Published posts for a student's browse view (saved ones highlighted). */
    List<PlacementDto> listOpportunities(User user, String skill, boolean savedOnly);

    PlacementDto getOpportunity(User user, Long id);

    /** Distinct skill keywords across published posts (for the filter dropdown). */
    List<String> listDistinctSkills();

    /** Bookmark/unbookmark an opportunity for the student. Returns true if now saved. */
    boolean toggleSave(User user, Long placementId);

    long publishedCount();
}
