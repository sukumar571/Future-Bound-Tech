package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trainers")
public class Trainer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String expertise;
    
    @Size(max = 1000)
    @Column(length = 1000)
    private String bio;

    /** Years of professional experience, admin-entered. Never inferred. */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /** Stored photo path/URL served from /uploads, or an external URL. */
    @Size(max = 500)
    @Column(name = "photo_url", length = 500)
    private String photoUrl;
    
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL)
    private List<Course> courses = new ArrayList<>();
    
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL)
    private List<Batch> batches = new ArrayList<>();

}
