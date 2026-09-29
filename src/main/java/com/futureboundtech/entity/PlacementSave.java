package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A "saved opportunity" bookmark linking a student to a placement post (Phase 17).
 * The unique constraint on (student_id, placement_id) makes duplicates impossible
 * at the DB level while keeping the project's surrogate-key BaseEntity convention.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "placement_saves",
        uniqueConstraints = @UniqueConstraint(name = "uk_placement_save", columnNames = {"student_id", "placement_id"}))
public class PlacementSave extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_id", nullable = false)
    private Placement placement;
}
