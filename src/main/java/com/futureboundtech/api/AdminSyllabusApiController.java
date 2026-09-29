package com.futureboundtech.api;

import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.LessonRequest;
import com.futureboundtech.service.SyllabusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Lesson authoring under a module. Paths are absolute so they sit alongside (not under) /api/admin/courses. */
@RestController
@RequiredArgsConstructor
public class AdminSyllabusApiController {

    private final SyllabusService syllabusService;

    @PostMapping("/api/admin/modules/{moduleId}/lessons")
    public ResponseEntity<ApiResponse<LessonDto>> addLesson(@PathVariable Long moduleId,
                                                            @Valid @RequestBody LessonRequest request) {
        LessonDto saved = syllabusService.createLesson(moduleId, request.toDto());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }

    @PutMapping("/api/admin/lessons/{lessonId}")
    public ApiResponse<LessonDto> updateLesson(@PathVariable Long lessonId,
                                               @Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok("Lesson updated", syllabusService.updateLesson(lessonId, request.toDto()));
    }
}
