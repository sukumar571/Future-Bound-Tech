package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentClassDto {

    private Long id;
    private String topic;
    private Long courseId;
    private String courseTitle;
    private String batchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String mode;          // ONLINE / OFFLINE
    private String modeLabel;
    private String meetingLink;
    private String classroomDetails;
    private String status;   // SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED
    private boolean upcoming;
    private boolean inProgress;
}
