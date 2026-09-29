package com.futureboundtech.service.impl;

import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.FileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/**
 * Stores uploads across two physical roots:
 * <ul>
 *   <li>{@code publicRoot} ({@code app.upload-dir}) — branding, course thumbnails,
 *       trainer/student photos. Served statically at {@code /uploads/**}.</li>
 *   <li>{@code privateRoot} ({@code app.private-upload-dir}) — lesson notes, assignment
 *       attachments, submissions. Kept OUTSIDE the web-served folder and reachable only
 *       through ownership-checked endpoints.</li>
 * </ul>
 * Stored DB values stay as logical relative paths (e.g. {@code lessons/uuid.pdf}); the
 * physical root is chosen by prefix at read/delete time.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> PRIVATE_PREFIXES = Set.of(
            LESSON_RESOURCE_PREFIX, SUBMISSION_RESOURCE_PREFIX, ASSIGNMENT_RESOURCE_PREFIX);

    private final Path publicRoot;
    private final Path privateRoot;

    public FileStorageServiceImpl(@Value("${app.upload-dir:./uploads}") String uploadDir,
                                  @Value("${app.private-upload-dir:./data/private-uploads}") String privateDir) {
        this.publicRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.privateRoot = Paths.get(privateDir).toAbsolutePath().normalize();
    }

    // ==================== Public image assets ====================

    @Override
    public String storeCourseThumbnail(MultipartFile file) {
        return storeImage(file, "courses", "Thumbnail", FileValidator.IMAGE_TYPES);
    }

    @Override
    public String storeTrainerPhoto(MultipartFile file) {
        return storeImage(file, "trainers", "Photo", FileValidator.IMAGE_TYPES);
    }

    @Override
    public String storeStudentPhoto(MultipartFile file) {
        return storeImage(file, "avatars", "Photo", FileValidator.IMAGE_TYPES);
    }

    @Override
    public String storeLogo(MultipartFile file) {
        return storeImage(file, "branding", "Logo", FileValidator.IMAGE_TYPES);
    }

    @Override
    public String storeFavicon(MultipartFile file) {
        return storeImage(file, "branding", "Favicon", FileValidator.FAVICON_TYPES);
    }

    /** Shared public-image storage: validate, UUID-name, write under the public root. */
    private String storeImage(MultipartFile file, String directoryName, String label, Set<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String declaredExt = FileValidator.validate(file, label, allowedTypes, null,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_IMAGE_BYTES);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = imageExtension(contentType, declaredExt);
        String filename = FileValidator.safeName(extension);
        return store(file, publicRoot, directoryName, filename, label);
    }

    // ==================== Private document uploads ====================

    @Override
    public String storeLessonResource(MultipartFile file) {
        return storePrivateDocument(file, "lessons", LESSON_RESOURCE_PREFIX, "Resource");
    }

    @Override
    public String storeSubmissionFile(MultipartFile file) {
        return storePrivateDocument(file, "submissions", SUBMISSION_RESOURCE_PREFIX, "Submission file");
    }

    @Override
    public String storeAssignmentAttachment(MultipartFile file) {
        return storePrivateDocument(file, "assignments", ASSIGNMENT_RESOURCE_PREFIX, "Attachment");
    }

    private String storePrivateDocument(MultipartFile file, String directoryName, String prefix, String label) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = FileValidator.validate(file, label, null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES);
        String filename = FileValidator.safeName(extension);
        store(file, privateRoot, directoryName, filename, label);
        return prefix + filename;
    }

    // ==================== Write helper ====================

    /** Copies an input stream into a freshly-created, UUID-named file under the given root. */
    private String store(MultipartFile file, Path root, String directoryName, String filename, String label) {
        Path directory = root.resolve(directoryName);
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("Invalid upload target.");
        }
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return directoryName + "/" + filename;
        } catch (IOException ex) {
            throw new BusinessException("Could not store the " + label.toLowerCase(Locale.ROOT) + ". Please try again.");
        }
    }

    // ==================== Read / delete with routing ====================

    @Override
    public boolean isPrivate(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String normalized = relativePath.replace("\\", "/");
        return PRIVATE_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private Path rootFor(String relativePath) {
        return isPrivate(relativePath) ? privateRoot : publicRoot;
    }

    @Override
    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        deleteUnder(rootFor(relativePath), relativePath);
        if (isPrivate(relativePath)) {
            // Backward-compat: legacy private files may still sit under the public root.
            deleteUnder(publicRoot, relativePath);
        }
    }

    private void deleteUnder(Path root, String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A failed cleanup must never fail the owning business operation.
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("File is missing.");
        }
        Resource resource = resolve(rootFor(relativePath), relativePath);
        if (resource == null && isPrivate(relativePath)) {
            // Legacy fallback for private files uploaded before the split.
            resource = resolve(publicRoot, relativePath);
        }
        if (resource == null) {
            throw new ResourceNotFoundException("File not found.");
        }
        return resource;
    }

    private Resource resolve(Path root, String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.isReadable(target)) {
            return null;
        }
        try {
            Resource resource = new UrlResource(target.toUri());
            return (resource.exists() && resource.isReadable()) ? resource : null;
        } catch (MalformedURLException ignored) {
            return null;
        }
    }

    // ==================== Small helpers ====================

    /** Chooses a stored image extension from the (trusted) content type, else a sane default. */
    private String imageExtension(String contentType, String declaredExt) {
        switch (contentType) {
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            case "image/gif":
                return "gif";
            case "image/jpeg":
                return "jpg";
            case "image/x-icon":
            case "image/vnd.microsoft.icon":
                return "ico";
            case "image/svg+xml":
                return "svg";
            default:
                return declaredExt.isBlank() ? "jpg" : declaredExt;
        }
    }
}
