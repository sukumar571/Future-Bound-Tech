package com.futureboundtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.futureboundtech.enums.MentorRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One persisted turn of a Future Mentor conversation.
 *
 * <p>Privacy: rows are always scoped to a single {@link Student} and are only
 * written when history storage is enabled. Free-text content uses a TEXT column so
 * MySQL does not hit the InnoDB row-size limit. A student can list or wipe their
 * own history; nothing here is ever exposed to other students or to the public.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mentor_messages",
        indexes = @Index(name = "idx_mentor_student_time", columnList = "student_id, created_at"))
public class MentorMessage extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MentorRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Whether this turn was produced by a real AI provider or the local fallback. */
    private boolean aiGenerated;

    /** Short label of the answering provider (e.g. "openai", "local"). */
    @Column(length = 40)
    private String provider;
}
