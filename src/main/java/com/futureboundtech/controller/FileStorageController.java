package com.futureboundtech.controller;

import com.futureboundtech.dto.TrainerSubmissionDto;
import com.futureboundtech.entity.LessonResource;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.FileValidator;
import com.futureboundtech.service.StudentDashboardService;
import com.futureboundtech.service.SyllabusService;
import com.futureboundtech.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Single, ownership-aware entry point for downloading every private uploaded file
 * (lesson notes, assignment attachments, student submissions). Private files are
 * stored outside the web-served folder, so they are only ever reachable through the
 * checks below — never by guessing a static URL.
 *
 * <p>Each handler delegates the authorization decision to the existing service
 * methods, then serves the bytes from disk. External (http/https) resource links are
 * answered with a 302 redirect rather than being proxied.</p>
 */
@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final SyllabusService syllabusService;
    private final StudentDashboardService studentDashboardService;
    private final TrainerService trainerService;
    private final FileStorageService fileStorageService;

    // ==================== Lesson resources ====================

    @GetMapping("/student/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<Resource> downloadLessonResource(@PathVariable Long lessonId,
                                                           @PathVariable Long resourceId,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        // Enrollment check: only students enrolled in the lesson's course pass here.
        LessonResource resource = syllabusService.authorizeResource(lessonId, resourceId, principal.getUser());
        return serveResource(resource);
    }

    @GetMapping("/staff/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<Resource> previewLessonResource(@PathVariable Long lessonId,
                                                          @PathVariable Long resourceId,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        // ADMIN or the owning TRAINER only.
        LessonResource resource = syllabusService.authorizeResourcePreview(lessonId, resourceId, principal.getUser());
        return serveResource(resource);
    }

    private ResponseEntity<Resource> serveResource(LessonResource resource) {
        String url = resource.getFileUrl();
        if (url == null || url.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (!url.startsWith(FileStorageService.LESSON_RESOURCE_PREFIX)) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
        }
        Resource file = syllabusService.loadStoredResourceFile(resource);
        String title = FileValidator.cleanDownloadName(resource.getTitle());
        int dot = url.lastIndexOf('.');
        String name = dot >= 0 ? title + url.substring(dot) : title;
        return serve(file, name);
    }

    // ==================== Assignment attachments ====================

    @GetMapping("/student/assignments/{id}/attachment")
    public ResponseEntity<Resource> downloadAssignmentAttachment(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        String path = studentDashboardService.authorizeAssignmentAttachment(id, principal.getUser());
        if (!path.startsWith(FileStorageService.ASSIGNMENT_RESOURCE_PREFIX)) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(path)).build();
        }
        return serve(fileStorageService.loadAsResource(path), leafName(path));
    }

    // ==================== Submissions (trainer only) ====================

    @GetMapping("/trainer/submissions/{id}")
    public ResponseEntity<Resource> downloadSubmissionFile(@PathVariable Long id,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        TrainerSubmissionDto dto = trainerService.getSubmissionForDownload(principal.getUser(), id);
        String path = dto.getFileUrl();
        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (!path.startsWith(FileStorageService.SUBMISSION_RESOURCE_PREFIX)) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(path)).build();
        }
        return serve(fileStorageService.loadAsResource(path), leafName(path));
    }

    // ==================== Helpers ====================

    private ResponseEntity<Resource> serve(Resource file, String downloadName) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(downloadName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    private String leafName(String path) {
        return FileValidator.cleanDownloadName(path.substring(path.lastIndexOf('/') + 1));
    }
}
