package com.futureboundtech.api;

import com.futureboundtech.dto.StudentAssignmentDto;
import com.futureboundtech.dto.TrainerSubmissionDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.StudentDashboardService;
import com.futureboundtech.service.TrainerService;
import com.futureboundtech.util.ApiPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Assignments: students list and submit; trainers review submissions. */
@RestController
@RequiredArgsConstructor
public class AssignmentApiController {

    private final StudentDashboardService studentDashboardService;
    private final TrainerService trainerService;

    @GetMapping("/api/student/assignments")
    public ApiResponse<PageDto<StudentAssignmentDto>> studentAssignments(@AuthenticationPrincipal CustomUserDetails principal,
                                                                        @PageableDefault(size = 20) Pageable pageable) {
        List<StudentAssignmentDto> assignments = studentDashboardService.getAssignments(principal.getUser());
        return ApiResponse.ok(ApiPaging.paginate(assignments, pageable));
    }

    @PostMapping(path = "/api/student/assignments/{id}/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> submit(@PathVariable Long id,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "note", required = false) String note) {
        studentDashboardService.submitAssignment(principal.getUser(), id, file, note);
        return ApiResponse.ok("Submission received", null);
    }

    @GetMapping("/api/trainer/submissions")
    public ApiResponse<PageDto<TrainerSubmissionDto>> submissions(@AuthenticationPrincipal CustomUserDetails principal,
                                                                 @RequestParam("assignmentId") Long assignmentId,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        List<TrainerSubmissionDto> submissions = trainerService.listSubmissions(principal.getUser(), assignmentId);
        return ApiResponse.ok(ApiPaging.paginate(submissions, pageable));
    }
}
