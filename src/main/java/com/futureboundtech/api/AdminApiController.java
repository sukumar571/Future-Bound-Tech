package com.futureboundtech.api;

import com.futureboundtech.dto.AdminStatsDto;
import com.futureboundtech.dto.StudentDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.PaymentService;
import com.futureboundtech.util.ApiPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin dashboards, student directory and reporting over JSON. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;
    private final PaymentService paymentService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminStatsDto> dashboard() {
        return ApiResponse.ok(adminService.dashboardStats());
    }

    @GetMapping("/students")
    public ApiResponse<PageDto<StudentDto>> students(@RequestParam(value = "q", required = false) String query,
                                                    @RequestParam(value = "active", required = false) Boolean active,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        List<StudentDto> students = adminService.listStudents(query, active);
        return ApiResponse.ok(ApiPaging.paginate(students, pageable));
    }

    @GetMapping("/reports")
    public ApiResponse<Map<String, Object>> reports() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("payments", paymentService.adminSummary());
        report.put("stats", adminService.dashboardStats());
        return ApiResponse.ok(report);
    }
}
