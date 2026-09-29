package com.futureboundtech.dto;

import com.futureboundtech.enums.ClassMode;
import com.futureboundtech.enums.ClassStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Trainer-scoped create/edit form for scheduling an online or offline class. */
@Data
@Accessors(chain = true)
public class TrainerClassFormDto {

    private Long id;

    @NotBlank(message = "Topic is required")
    @Size(max = 200, message = "Topic must be 200 characters or fewer")
    private String topic;

    @NotNull(message = "Select one of your batches")
    private Long batchId;
    private String batchName;
    private String courseTitle;

    @NotNull(message = "Start date/time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private ClassMode mode = ClassMode.ONLINE;

    @Size(max = 500, message = "Meeting link must be 500 characters or fewer")
    private String meetingLink;

    @Size(max = 300, message = "Classroom details must be 300 characters or fewer")
    private String classroomDetails;

    private ClassStatus status = ClassStatus.SCHEDULED;
}
