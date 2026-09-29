package com.futureboundtech.dto;

import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.ClassMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Live class read model and create/edit form for the admin portal. */
@Data
@Accessors(chain = true)
public class LiveClassDto {

    private Long id;

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotNull(message = "Batch is required")
    private Long batchId;
    private String batchName;
    private String courseTitle;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private ClassMode mode = ClassMode.ONLINE;
    private String modeLabel;

    private String meetingLink;

    private String classroomDetails;

    private ClassStatus status = ClassStatus.SCHEDULED;
    private String statusLabel;

    private boolean upcoming;
    private String startLabel;
}
