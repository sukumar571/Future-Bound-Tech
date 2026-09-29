package com.futureboundtech.dto;

import com.futureboundtech.enums.BatchEnrollmentStatus;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.TrainingMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Batch read model and create/edit form for the admin portal. */
@Data
@Accessors(chain = true)
public class BatchDto {

    private Long id;

    @NotBlank(message = "Batch name is required")
    private String batchName;

    @NotNull(message = "Course is required")
    private Long courseId;
    private String courseTitle;
    private String courseSlug;

    @NotNull(message = "Trainer is required")
    private Long trainerId;
    private String trainerName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    private TrainingMode mode;

    private ClassStatus status = ClassStatus.SCHEDULED;
    private String statusLabel;

    private BatchEnrollmentStatus enrollmentStatus = BatchEnrollmentStatus.OPEN;
    private String enrollmentStatusLabel;

    private boolean published = true;

    @Min(value = 1, message = "Maximum seats must be at least 1")
    private Integer maxSeats;

    @Size(max = 500, message = "Meeting link is too long")
    private String meetingLink;

    @Size(max = 500, message = "Classroom address is too long")
    private String classroomAddress;

    @Size(max = 50, message = "Room number is too long")
    private String roomNumber;

    private String notes;

    private long enrolledCount;
    private long seatsAvailable;
}
