package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** Attendance roster for one scheduled class — students of the owning batch. */
@Data
@Accessors(chain = true)
public class TrainerClassRosterDto {

    private Long classId;
    private String topic;
    private String batchName;
    private String courseTitle;
    private String startLabel;
    private String statusLabel;
    private long presentCount;
    private long totalCount;

    private List<TrainerAttendanceRowDto> roster = new ArrayList<>();
}
