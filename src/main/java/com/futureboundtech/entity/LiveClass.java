package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.ClassMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "liveclasss")
public class LiveClass extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnore
    private Batch batch;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ClassMode mode = ClassMode.ONLINE;

    private String meetingLink;

    @Column(length = 300)
    private String classroomDetails;

    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.SCHEDULED;
    
    @OneToMany(mappedBy = "liveClass", cascade = CascadeType.ALL)
    private List<Attendance> attendances = new ArrayList<>();

}
