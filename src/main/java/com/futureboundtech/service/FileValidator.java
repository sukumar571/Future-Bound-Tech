package com.futureboundtech.service;

import com.futureboundtech.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Central, framework-free rules for every file the platform accepts: type, size,
 * dangerous-extension / dangerous-MIME blacklists, and safe (never user-derived)
 * file naming. Original filenames are treated only as untrusted display text.
 *
 * <p>This is the single source of truth for upload validation — every
 * {@code store*} method in {@link com.futureboundtech.service.FileStorageService}
 * funnels through {@link #validate}.</p>
 */
public final class FileValidator {

    private FileValidator() {
    }

    /** Cap for avatar / logo / thumbnail / favicon images. */
    public static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024; // 2 MB

    /** Cap for lesson notes, assignment attachments and submissions. */
    public static final long MAX_DOC_BYTES = 10L * 1024 * 1024;  // 10 MB

    public static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    public static final Set<String> FAVICON_TYPES = Set.of(
            "image/x-icon", "image/vnd.microsoft.icon", "image/png", "image/jpeg",
            "image/webp", "image/gif", "image/svg+xml");

    /** Extensions we are willing to store for document-style uploads. */
    public static final Set<String> DOC_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "csv", "zip",
            "jpg", "jpeg", "png", "gif", "webp",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "py", "java", "js", "ts", "html", "css", "sql", "json", "xml", "yml", "yaml", "ipynb");

    /** Never accepted regardless of declared MIME — OS executables / active content. */
    public static final Set<String> BANNED_EXTENSIONS = Set.of(
            "exe", "msi", "bat", "cmd", "com", "scr", "dll", "so", "jar", "class",
            "sh", "ps1", "vbs", "wsf", "app", "apk", "php", "jsp", "jspx", "asp", "aspx", "swf", "cgi");

    /** Declared content types that are always rejected as dangerous. */
    public static final Set<String> BANNED_MIME_TYPES = Set.of(
            "application/x-msdownload", "application/x-msdos-program", "application/x-executable",
            "application/x-elf", "application/x-sh", "application/x-shellscript",
            "text/x-script.shell", "application/x-macbinary", "application/java-vm",
            "application/x-msi", "application/vnd.microsoft.portable-executable",
            "application/x-www-form-urlencoded");

    /**
     * Validates a file and returns its safe, lowercased extension (no leading dot).
     *
     * @param label      human name used in error messages (e.g. "Thumbnail", "Submission file")
     * @param allowedMime accepted content types, or {@code null} to skip MIME allow-listing
     *                    (documents rely on the extension allow-list instead)
     * @param allowedExt  accepted extensions, or {@code null} to skip extension allow-listing
     * @param bannedExt   hard-rejected extensions (typically {@link #BANNED_EXTENSIONS})
     * @param bannedMime  hard-rejected content types (typically {@link #BANNED_MIME_TYPES})
     * @param maxBytes    maximum file size in bytes
     * @throws BusinessException with a user-facing message when the file is rejected
     */
    public static String validate(MultipartFile file, String label,
                                  Set<String> allowedMime, Set<String> allowedExt,
                                  Set<String> bannedExt, Set<String> bannedMime, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a " + lowercase(label) + " to upload.");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(capitalize(label) + " is too large. The maximum allowed size is "
                    + (maxBytes / (1024 * 1024)) + " MB.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (bannedMime != null && bannedMime.contains(contentType)) {
            throw new BusinessException(capitalize(label) + " files of this type are not allowed for security reasons.");
        }
        if (allowedMime != null && !allowedMime.contains(contentType)) {
            throw new BusinessException(capitalize(label) + " must be one of: " + join(allowedMime) + ".");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (bannedExt != null && bannedExt.contains(ext)) {
            throw new BusinessException(capitalize(label) + ": executable or active-content files are not allowed.");
        }
        if (allowedExt != null && !allowedExt.contains(ext)) {
            throw new BusinessException(capitalize(label) + ": this file type is not allowed. Use one of: "
                    + join(allowedExt) + ".");
        }
        return ext;
    }

    /** Generates a safe stored name; never derived from the (untrusted) original filename. */
    public static String safeName(String extension) {
        String ext = extension == null || extension.isBlank() ? "" : "." + extension.toLowerCase(Locale.ROOT);
        return UUID.randomUUID() + ext;
    }

    /** Strips path separators and control characters from a name used for download. */
    public static String cleanDownloadName(String name) {
        if (name == null || name.isBlank()) {
            return "download";
        }
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        return cleaned.isEmpty() ? "download" : cleaned;
    }

    /** Lowercased extension without the leading dot, or "" when there is none. */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String join(Set<String> values) {
        StringJoiner sj = new StringJoiner(", ");
        for (String v : new java.util.TreeSet<>(values)) {
            sj.add(v);
        }
        return sj.toString();
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String lowercase(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
