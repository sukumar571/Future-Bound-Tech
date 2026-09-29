package com.futureboundtech.entity;

import com.futureboundtech.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
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
@Table(name = "assignments")
public class Assignment extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;
    
    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnore
    private Batch batch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @JsonIgnore
    private Lesson lesson;

    private LocalDateTime dueDate;

    /** Highest mark a submission can be awarded. Null = unbounded. */
    @Min(0)
    @Column(name = "max_marks")
    private Integer maxMarks;

    /** Trainer-supplied resource students can download (relative path under /uploads). */
    @Column(name = "attachment_url")
    private String attachmentUrl;

    /** Original filename of the attachment, shown to students on the download link. */
    @Column(name = "attachment_name")
    private String attachmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PUBLISHED;

    /** When false, submissions are rejected on the backend once the deadline passes. */
    @Column(name = "allow_late")
    @Builder.Default
    private boolean allowLate = false;
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    private List<Submission> submissions = new ArrayList<>();

    public boolean isPublished() {
        return status == AssignmentStatus.PUBLISHED;
    }
}
