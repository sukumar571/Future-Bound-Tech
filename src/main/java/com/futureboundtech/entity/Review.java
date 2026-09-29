package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    /** Null for curated/demo testimonials that are not tied to a real enrollment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    /** Display name for curated testimonials (ignored for student reviews). */
    @Size(max = 120)
    @Column(name = "author_name", length = 120)
    private String authorName;

    /** Supporting context line, e.g. course or batch label shown under the name. */
    @Size(max = 160)
    @Column(name = "context_label", length = 160)
    private String contextLabel;

    @Min(1)
    private Integer rating;
    
    @Size(max = 1000)
    @Column(length = 1000)
    private String comment;
    
    private boolean isApproved = false;

    /** Clearly-labelled placeholder shown until a real, verified review exists. */
    @Column(name = "is_demo", nullable = false)
    private boolean isDemo = false;

    /** Whether the testimonial is displayed on the public site. */
    @Column(nullable = false)
    private boolean published = false;

}
