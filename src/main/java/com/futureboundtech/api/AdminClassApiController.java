package com.futureboundtech.api;

import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.ClassRequest;
import com.futureboundtech.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin scheduling of live classes. */
@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
public class AdminClassApiController {

    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody ClassRequest request) {
        adminService.createLiveClass(request.toDto());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Class scheduled", null));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        adminService.updateLiveClass(id, request.toDto());
        return ApiResponse.ok("Class updated", null);
    }
}
