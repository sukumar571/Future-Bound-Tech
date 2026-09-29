package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import jakarta.validation.constraints.*;

/**
 * A published question/answer shown on the public FAQ page. Admin-managed and
 * individually publishable so the site can stage content without deleting it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "faqitems")
public class FaqItem extends BaseEntity {

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String question;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String answer;

    @Size(max = 60)
    @Column(length = 60)
    private String category;

    @Column(nullable = false)
    private boolean published = true;

    /** Lower numbers sort first; ties fall back to creation order. */
    private Integer sortOrder = 0;
}
