package com.futureboundtech.unit;

import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.FileValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the framework-free upload validator (Phase 25 security surface):
 * extension whitelist, executable blacklist, size caps and safe naming.
 */
class FileValidatorTest {

    private static MockMultipartFile file(String name, String mime, String content) {
        return new MockMultipartFile("file", name, mime,
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile empty(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[0]);
    }

    @Test
    @DisplayName("null / empty uploads are rejected")
    void rejectsEmpty() {
        assertThrows(BusinessException.class, () -> FileValidator.validate(
                null, "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES));
        assertThrows(BusinessException.class, () -> FileValidator.validate(
                empty("notes.pdf"), "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES));
    }

    @Test
    @DisplayName("oversized uploads are rejected")
    void rejectsTooLarge() {
        MockMultipartFile big = file("notes.pdf", "application/pdf", "x".repeat(2048));
        assertThrows(BusinessException.class, () -> FileValidator.validate(
                big, "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, 1024));
    }

    @Test
    @DisplayName("executables are rejected even when the extension list would allow them")
    void rejectsBannedExtension() {
        MockMultipartFile exe = file("payload.exe", "application/octet-stream", "MZ");
        BusinessException ex = assertThrows(BusinessException.class, () -> FileValidator.validate(
                exe, "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES));
        assertTrue(ex.getMessage().toLowerCase().contains("executable"));
    }

    @Test
    @DisplayName("banned declared MIME types are rejected before the extension check")
    void rejectsBannedMime() {
        MockMultipartFile f = file("report.pdf", "application/x-msdownload", "x");
        assertThrows(BusinessException.class, () -> FileValidator.validate(
                f, "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES));
    }

    @Test
    @DisplayName("an extension outside the whitelist is rejected")
    void rejectsNotAllowedExtension() {
        MockMultipartFile f = file("notes.xyz", "text/plain", "x");
        BusinessException ex = assertThrows(BusinessException.class, () -> FileValidator.validate(
                f, "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES));
        assertTrue(ex.getMessage().toLowerCase().contains("not allowed"));
    }

    @Test
    @DisplayName("a whitelisted document passes and its lower-cased extension is returned")
    void acceptsDocument() {
        String ext = FileValidator.validate(
                file("syllabus.pdf", "application/pdf", "x"), "Lesson", null, FileValidator.DOC_EXTENSIONS,
                FileValidator.BANNED_EXTENSIONS, FileValidator.BANNED_MIME_TYPES, FileValidator.MAX_DOC_BYTES);
        assertEquals("pdf", ext);
    }

    @Test
    @DisplayName("uppercase extensions are normalised")
    void upperCaseExtension() {
        assertEquals("pdf", FileValidator.extensionOf("Report.PDF"));
    }

    @Test
    @DisplayName("extensionOf handles null, missing, trailing-dot and multi-dot names")
    void extensionEdges() {
        assertEquals("", FileValidator.extensionOf(null));
        assertEquals("", FileValidator.extensionOf("README"));
        assertEquals("", FileValidator.extensionOf("archive."));
        assertEquals("gz", FileValidator.extensionOf("backup.tar.gz"));
    }

    @Test
    @DisplayName("safeName is a UUID, never derived from the original filename")
    void safeNameIsOpaque() {
        String name = FileValidator.safeName("pdf");
        assertTrue(name.endsWith(".pdf"));
        assertDoesNotThrow(() -> java.util.UUID.fromString(name.substring(0, name.lastIndexOf('.'))));
        assertEquals(36 + ".pdf".length(), name.length());

        String noExt = FileValidator.safeName(null);
        assertDoesNotThrow(() -> java.util.UUID.fromString(noExt));
        assertFalse(noExt.contains("."));
    }

    @Test
    @DisplayName("cleanDownloadName strips path separators and falls back for blanks")
    void cleanDownloadName() {
        assertEquals("download", FileValidator.cleanDownloadName(null));
        assertEquals("download", FileValidator.cleanDownloadName("   "));
        assertEquals("report final.pdf", FileValidator.cleanDownloadName("report final.pdf"));
        String traversal = FileValidator.cleanDownloadName("../../windows/system32/cmd.exe");
        assertFalse(traversal.contains("/"));
        assertFalse(traversal.contains("\\"));
    }
}
