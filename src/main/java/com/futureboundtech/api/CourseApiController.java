package com.futureboundtech.api;

import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.dto.CourseModuleDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.SyllabusService;
import com.futureboundtech.util.ApiPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public course catalog: browse, inspect and preview the syllabus. */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseApiController {

    private final CourseService courseService;
    private final SyllabusService syllabusService;

    @GetMapping
    public ApiResponse<PageDto<CourseDto>> list(@RequestParam(value = "q", required = false) String query,
                                                @RequestParam(value = "category", required = false) CourseCategory category,
                                                @RequestParam(value = "level", required = false) CourseLevel level,
                                                @PageableDefault(size = 20) Pageable pageable) {
        List<CourseDto> courses = courseService.searchPublic(query, category, level);
        return ApiResponse.ok(ApiPaging.paginate(courses, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseDto> detail(@PathVariable Long id) {
        CourseDto course = courseService.findById(id);
        if (course.getStatus() != CourseStatus.PUBLISHED || !course.isPublicListed()) {
            throw new ResourceNotFoundException("Course not found.");
        }
        return ApiResponse.ok(course);
    }

    @GetMapping("/{courseId}/syllabus")
    public ApiResponse<List<CourseModuleDto>> syllabus(@PathVariable Long courseId) {
        return ApiResponse.ok(syllabusService.getPublicPreview(courseId));
    }
}
