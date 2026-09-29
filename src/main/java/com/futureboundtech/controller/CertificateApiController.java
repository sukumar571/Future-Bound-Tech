package com.futureboundtech.controller;

import com.futureboundtech.dto.CertificateVerifyDto;
import com.futureboundtech.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public JSON endpoint so third parties (employers, colleges) can programmatically
 * verify a certificate number. Returns 200 with {@code valid=false} for unknown,
 * revoked or not-yet-approved numbers.
 */
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateApiController {

    private final CertificateService certificateService;

    @GetMapping("/verify")
    public CertificateVerifyDto verify(@RequestParam(name = "number", required = false) String number) {
        return certificateService.verify(number);
    }
}
