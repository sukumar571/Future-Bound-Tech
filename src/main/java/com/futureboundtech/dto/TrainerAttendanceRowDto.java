package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** One row of the trainer's mark-attendance table for a scheduled class. */
@Data
@Accessors(chain = true)
public class TrainerAttendanceRowDto {
    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private boolean present;
}
