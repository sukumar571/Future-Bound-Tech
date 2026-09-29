package com.futureboundtech.api;

import com.futureboundtech.dto.StudentEnrollmentDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.EnrollmentRequest;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.StudentDashboardService;
import com.futureboundtech.util.ApiPaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Enrollment: join a course and review your own enrollment history. */
@RestController
@RequiredArgsConstructor
public class EnrollmentApiController {

    private final CourseService courseService;
    private final StudentDashboardService studentDashboardService;

    @PostMapping("/api/enrollments")
    public ResponseEntity<ApiResponse<StudentEnrollmentDto>> enroll(@Valid @RequestBody EnrollmentRequest request,
                                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        Long courseId = courseService.findPublicBySlug(request.getCourseSlug()).getId();
        courseService.enrollStudent(request.getCourseSlug(), principal.getUser(), request.getBatchId());
        StudentEnrollmentDto match = studentDashboardService.getEnrollmentHistory(principal.getUser()).stream()
                .filter(e -> courseId.equals(e.getCourseId()))
                .findFirst()
                .orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Enrolled", match));
    }

    @GetMapping("/api/student/enrollments")
    public ApiResponse<PageDto<StudentEnrollmentDto>> myEnrollments(@AuthenticationPrincipal CustomUserDetails principal,
                                                                    @PageableDefault(size = 20) Pageable pageable) {
        List<StudentEnrollmentDto> history = studentDashboardService.getEnrollmentHistory(principal.getUser());
        return ApiResponse.ok(ApiPaging.paginate(history, pageable));
    }
}
