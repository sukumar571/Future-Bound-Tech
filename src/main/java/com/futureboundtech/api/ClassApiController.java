package com.futureboundtech.api;

import com.futureboundtech.dto.LiveClassDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.util.ApiPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Public upcoming-class schedule (no auth required). */
@RestController
@RequiredArgsConstructor
public class ClassApiController {

    private final AdminService adminService;

    @GetMapping("/api/classes/upcoming")
    public ApiResponse<PageDto<LiveClassDto>> upcoming(@PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        List<LiveClassDto> upcoming = adminService.listLiveClasses().stream()
                .filter(c -> c.getStartTime() != null && c.getStartTime().isAfter(now))
                .toList();
        return ApiResponse.ok(ApiPaging.paginate(upcoming, pageable));
    }
}
