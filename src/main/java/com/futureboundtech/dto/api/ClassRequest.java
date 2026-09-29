package com.futureboundtech.dto.api;

import com.futureboundtech.dto.LiveClassDto;
import com.futureboundtech.enums.ClassMode;
import com.futureboundtech.enums.ClassStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Create/update payload for a live class scheduled against a batch. */
@Data
@Accessors(chain = true)
public class ClassRequest {

    @NotBlank(message = "Topic is required")
    @Size(max = 200, message = "Topic is too long")
    private String topic;

    @NotNull(message = "Batch is required")
    private Long batchId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private ClassMode mode = ClassMode.ONLINE;

    @Size(max = 500, message = "Meeting link is too long")
    private String meetingLink;

    @Size(max = 500, message = "Classroom details are too long")
    private String classroomDetails;

    private ClassStatus status = ClassStatus.SCHEDULED;

    public LiveClassDto toDto() {
        return new LiveClassDto()
                .setTopic(topic)
                .setBatchId(batchId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setMode(mode)
                .setMeetingLink(meetingLink)
                .setClassroomDetails(classroomDetails)
                .setStatus(status);
    }
}
