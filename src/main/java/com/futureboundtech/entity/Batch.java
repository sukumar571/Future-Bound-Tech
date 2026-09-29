package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.BatchEnrollmentStatus;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "batchs")
public class Batch extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String batchName;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    @JsonIgnore
    private Trainer trainer;

    private LocalDate startDate;
    private LocalDate endDate;
    
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private TrainingMode mode;
    
    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    private BatchEnrollmentStatus enrollmentStatus = BatchEnrollmentStatus.OPEN;

    @Builder.Default
    private boolean published = true;

    @Min(1)
    private Integer maxSeats;

    @Size(max = 500)
    @Column(length = 500)
    private String meetingLink;

    @Size(max = 500)
    @Column(length = 500)
    private String classroomAddress;

    @Size(max = 50)
    @Column(length = 50)
    private String roomNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<LiveClass> liveClasses = new ArrayList<>();

}
