package com.futureboundtech.api;

import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.dto.CourseModuleDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.CourseModuleRequest;
import com.futureboundtech.dto.api.CourseRequest;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.SyllabusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin course management over JSON. Thumbnail uploads stay on the web UI (Phase 22). */
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseApiController {

    private final CourseService courseService;
    private final SyllabusService syllabusService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDto>> create(@Valid @RequestBody CourseRequest request) {
        CourseDto saved = courseService.create(request.toCourseDto(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseDto> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok("Course updated", courseService.update(id, request.toCourseDto(), null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.deleteSafely(id);
        return ApiResponse.ok("Course deleted", null);
    }

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<ApiResponse<CourseModuleDto>> addModule(@PathVariable Long courseId,
                                                                  @Valid @RequestBody CourseModuleRequest request) {
        CourseModuleDto saved = syllabusService.createModule(courseId, request.toDto());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }
}
