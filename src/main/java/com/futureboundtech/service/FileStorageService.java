package com.futureboundtech.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String LESSON_RESOURCE_PREFIX = "lessons/";

    String SUBMISSION_RESOURCE_PREFIX = "submissions/";

    String ASSIGNMENT_RESOURCE_PREFIX = "assignments/";

    /** Public avatar folder (student profile photos). */
    String AVATAR_PREFIX = "avatars/";

    String storeCourseThumbnail(MultipartFile file);

    String storeTrainerPhoto(MultipartFile file);

    String storeStudentPhoto(MultipartFile file);

    String storeLogo(MultipartFile file);

    String storeFavicon(MultipartFile file);

    String storeLessonResource(MultipartFile file);

    String storeSubmissionFile(MultipartFile file);

    String storeAssignmentAttachment(MultipartFile file);

    /** True when a stored relative path points at a private file (served only via authorized endpoints). */
    boolean isPrivate(String relativePath);

    void deleteIfExists(String relativePath);

    Resource loadAsResource(String relativePath);
}
