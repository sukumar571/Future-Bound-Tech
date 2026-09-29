package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A student's attendance record across their classes, with an overall summary. */
@Data
@Accessors(chain = true)
public class StudentAttendanceDto {

    private long totalClasses;
    private long presentClasses;
    private int attendancePercent;
    private List<Row> rows = new ArrayList<>();

    /** One class with the student's attendance status. */
    @Data
    @Accessors(chain = true)
    public static class Row {
        private Long liveClassId;
        private String topic;
        private String courseTitle;
        private String batchName;
        private LocalDateTime startTime;
        /** PRESENT / ABSENT / NOT_MARKED */
        private String status;
        private String statusLabel;
    }
}
