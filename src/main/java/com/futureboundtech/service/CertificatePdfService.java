package com.futureboundtech.service;

import com.futureboundtech.entity.Certificate;
import com.futureboundtech.entity.CertificateEligibility;
import com.futureboundtech.enums.CertificateStatus;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Draws the official Future Bound Tech certificate as a colorful, print-ready
 * A4-landscape PDF: layered brand-coloured frame, soft corner glows, gold seal
 * with ribbon, watermark and an authorised-signature block.
 */
@Slf4j
@Service
public class CertificatePdfService {

    private static final Color DEEP_NAVY = new Color(11, 16, 32);
    private static final Color ROYAL_BLUE = new Color(37, 99, 235);
    private static final Color ELECTRIC_PURPLE = new Color(124, 58, 237);
    private static final Color CYAN = new Color(6, 182, 212);
    private static final Color GOLD = new Color(212, 160, 23);
    private static final Color CORAL = new Color(244, 114, 182);
    private static final Color INK_GRAY = new Color(75, 85, 99);

    // Pre-blended "glow" tints (OpenPDF has no PdfGState transparency helpers).
    private static final Color PURPLE_GLOW = new Color(236, 227, 252);
    private static final Color CYAN_GLOW = new Color(214, 243, 250);
    private static final Color PINK_GLOW = new Color(253, 226, 241);
    private static final Color BLUE_GLOW = new Color(217, 232, 253);
    private static final Color WATERMARK_GRAY = new Color(230, 233, 238);
    private static final Color SPECIMEN_RED_SOFT = new Color(247, 190, 190);

    /** Page height of A4 landscape — used for top-down placement. */
    private static final float PAGE_H = PageSize.A4.rotate().getHeight();

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public byte[] render(Certificate certificate, String institutionName, String logoUrl,
                         CertificateEligibility config, String verificationUrl,
                         String instituteSignatory, String instituteSignatoryTitle, String certificateFooterNote) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            float w = document.getPageSize().getWidth();

            drawFrame(cb, w);
            drawWatermark(cb, w, certificate.getStatus());
            drawHeader(cb, w, institutionName, logoUrl);

            String title = StringUtils.hasText(config.getCertificateTitle())
                    ? config.getCertificateTitle() : "Certificate of Completion";
            drawBody(cb, w, title.toUpperCase(), certificate.getStudentName(),
                    certificate.getCourseTitle(), certificate.getIssueDate());

            drawSeal(cb, 165, 165);
            drawSignature(cb, w, config, instituteSignatory, instituteSignatoryTitle);
            drawFooter(cb, w, certificate.getCertificateNumber(), verificationUrl, certificateFooterNote);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Certificate PDF generation failed for {}", certificate.getCertificateNumber(), ex);
            throw new RuntimeException("Unable to generate the certificate PDF.", ex);
        }
    }

    // ------------------------------------------------------------------- frame

    private void drawFrame(PdfContentByte cb, float w) {
        float h = PAGE_H;
        cb.saveState();
        cb.setColorFill(PURPLE_GLOW);
        cb.circle(0, h, 170);
        cb.fill();
        cb.setColorFill(CYAN_GLOW);
        cb.circle(w, 0, 150);
        cb.fill();
        cb.setColorFill(PINK_GLOW);
        cb.circle(w, h, 90);
        cb.fill();
        cb.setColorFill(BLUE_GLOW);
        cb.circle(0, 0, 100);
        cb.fill();
        cb.restoreState();

        // Navy outer band, purple band and gold hairline, layered inward.
        fillRect(cb, 0, 0, w, h, DEEP_NAVY);
        fillRect(cb, 14, 14, w - 28, h - 28, Color.WHITE);
        fillRect(cb, 20, 20, w - 40, h - 40, ELECTRIC_PURPLE);
        fillRect(cb, 24.5f, 24.5f, w - 49, h - 49, Color.WHITE);
        strokeRect(cb, 32, 32, w - 32, h - 32, GOLD, 1.2f);

        cornerAccent(cb, 22, h - 22, CYAN);
        cornerAccent(cb, w - 22, h - 22, GOLD);
        cornerAccent(cb, 22, 22, GOLD);
        cornerAccent(cb, w - 22, 22, CORAL);
    }

    private void fillRect(PdfContentByte cb, float x, float y, float bw, float bh, Color color) {
        cb.saveState();
        cb.setColorFill(color);
        cb.rectangle(x, y, bw, bh);
        cb.fill();
        cb.restoreState();
    }

    private void strokeRect(PdfContentByte cb, float x0, float y0, float x1, float y1,
                            Color color, float width) {
        cb.saveState();
        cb.setColorStroke(color);
        cb.setLineWidth(width);
        cb.rectangle(x0, y0, x1 - x0, y1 - y0);
        cb.stroke();
        cb.restoreState();
    }

    private void cornerAccent(PdfContentByte cb, float x, float y, Color color) {
        cb.saveState();
        cb.setColorFill(color);
        cb.circle(x, y, 9);
        cb.fill();
        cb.setColorStroke(Color.WHITE);
        cb.setLineWidth(2);
        cb.circle(x, y, 9);
        cb.stroke();
        cb.restoreState();
    }

    private void drawWatermark(PdfContentByte cb, float w, CertificateStatus status) {
        cb.saveState();
        boolean specimen = status != CertificateStatus.ISSUED;
        String text = specimen ? "SPECIMEN - NOT ISSUED" : "FUTURE BOUND TECH";
        Color color = specimen ? SPECIMEN_RED_SOFT : WATERMARK_GRAY;
        float size = specimen ? 52 : 60;
        text(cb, BaseFont.HELVETICA_BOLD, size, w / 2f, PAGE_H / 2f - 25,
                Element.ALIGN_CENTER, text, color, 0.2f, 22f);
        cb.restoreState();
    }

    // ------------------------------------------------------------------ header

    private void drawHeader(PdfContentByte cb, float w, String institutionName, String logoUrl) {
        float top = PAGE_H;
        byte[] logo = tryLoadLogo(logoUrl);
        float nameY = top - 60;
        if (logo != null) {
            try {
                Image image = Image.getInstance(logo);
                image.scaleToFit(62, 62);
                image.setAbsolutePosition(w / 2f - image.getScaledWidth() / 2f, top - 92);
                cb.addImage(image);
                nameY = top - 104;
            } catch (Exception ex) {
                log.warn("Certificate logo could not be embedded: {}", ex.getMessage());
            }
        }
        text(cb, BaseFont.HELVETICA_BOLD, 20, w / 2f, nameY, Element.ALIGN_CENTER,
                institutionName.toUpperCase(), DEEP_NAVY, 3.2f, 0f);
        text(cb, BaseFont.HELVETICA, 9.5f, w / 2f, nameY - 16, Element.ALIGN_CENTER,
                "COACHING INSTITUTE  ·  LEARNING MANAGEMENT SYSTEM", INK_GRAY, 1.6f, 0f);
    }

    /** Loads a raster logo from the upload dir or classpath; SVG/remote URLs are skipped. */
    private byte[] tryLoadLogo(String logoUrl) {
        if (!StringUtils.hasText(logoUrl)) {
            return null;
        }
        String lower = logoUrl.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            return null;
        }
        try {
            if (logoUrl.startsWith("/uploads/")) {
                Path file = Paths.get(uploadDir, logoUrl.substring("/uploads/".length()));
                return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
            }
            if (logoUrl.startsWith("/")) {
                ClassPathResource resource = new ClassPathResource("static" + logoUrl);
                if (resource.exists()) {
                    return resource.getInputStream().readAllBytes();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not load certificate logo {}: {}", logoUrl, ex.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------- body

    private void drawBody(PdfContentByte cb, float w, String title, String studentName,
                          String courseTitle, java.time.LocalDate issueDate) {
        float cx = w / 2f;
        text(cb, BaseFont.HELVETICA_BOLD, 26, cx, 460, Element.ALIGN_CENTER,
                title, ROYAL_BLUE, 2.4f, 0f);
        gradientRule(cb, cx - 90, 448, 180);

        text(cb, BaseFont.HELVETICA, 11, cx, 424, Element.ALIGN_CENTER,
                "THIS IS TO CERTIFY THAT", INK_GRAY, 2.6f, 0f);
        text(cb, BaseFont.TIMES_BOLDITALIC, 38, cx, 378, Element.ALIGN_CENTER,
                studentName, DEEP_NAVY, 0f, 0f);
        gradientRule(cb, cx - 160, 366, 320);

        text(cb, BaseFont.TIMES_ITALIC, 14, cx, 338, Element.ALIGN_CENTER,
                "has successfully completed the course", INK_GRAY, 0f, 0f);
        text(cb, BaseFont.HELVETICA_BOLD, 20, cx, 308, Element.ALIGN_CENTER,
                courseTitle, ELECTRIC_PURPLE, 1f, 0f);
        text(cb, BaseFont.HELVETICA, 10, cx, 282, Element.ALIGN_CENTER,
                "having met all institute eligibility requirements, verified by the administration",
                INK_GRAY, 0.5f, 0f);

        String dateLine = issueDate != null
                ? "Awarded on " + issueDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))
                : "Awaiting approval";
        text(cb, BaseFont.HELVETICA_BOLD, 10.5f, cx, 258, Element.ALIGN_CENTER,
                dateLine, DEEP_NAVY, 0.8f, 0f);

        // divider with diamond
        cb.saveState();
        cb.setColorStroke(GOLD);
        cb.setLineWidth(1.2f);
        cb.moveTo(cx - 130, 240);
        cb.lineTo(cx - 14, 240);
        cb.moveTo(cx + 14, 240);
        cb.lineTo(cx + 130, 240);
        cb.stroke();
        cb.setColorFill(ROYAL_BLUE);
        cb.moveTo(cx, 246);
        cb.lineTo(cx + 6, 240);
        cb.lineTo(cx, 234);
        cb.lineTo(cx - 6, 240);
        cb.closePath();
        cb.fill();
        cb.restoreState();
    }

    private void gradientRule(PdfContentByte cb, float x, float y, float width) {
        Color[] stops = {ROYAL_BLUE, ELECTRIC_PURPLE, CYAN, GOLD, CORAL};
        float seg = width / stops.length;
        cb.saveState();
        for (int i = 0; i < stops.length; i++) {
            cb.setColorFill(stops[i]);
            cb.rectangle(x + i * seg, y, seg - 2, 2.2f);
            cb.fill();
        }
        cb.restoreState();
    }

    // -------------------------------------------------------------------- seal

    private void drawSeal(PdfContentByte cb, float x, float y) {
        cb.saveState();
        cb.setColorFill(CORAL);
        cb.moveTo(x - 18, y - 22);
        cb.lineTo(x - 32, y - 64);
        cb.lineTo(x - 8, y - 50);
        cb.closePath();
        cb.fill();
        cb.setColorFill(ROYAL_BLUE);
        cb.moveTo(x + 18, y - 22);
        cb.lineTo(x + 32, y - 64);
        cb.lineTo(x + 8, y - 50);
        cb.closePath();
        cb.fill();

        cb.setColorFill(GOLD);
        cb.circle(x, y, 44);
        cb.fill();
        cb.setColorStroke(DEEP_NAVY);
        cb.setLineWidth(2);
        cb.circle(x, y, 36);
        cb.stroke();
        cb.setColorFill(DEEP_NAVY);
        cb.circle(x, y, 33);
        cb.fill();

        text(cb, BaseFont.HELVETICA_BOLD, 15, x, y + 6, Element.ALIGN_CENTER, "FBT", GOLD, 1f, 0f);
        text(cb, BaseFont.HELVETICA, 6.4f, x, y - 6, Element.ALIGN_CENTER, "OFFICIAL", Color.WHITE, 1.1f, 0f);
        text(cb, BaseFont.HELVETICA, 6.4f, x, y - 15, Element.ALIGN_CENTER, "RECORD", Color.WHITE, 1.1f, 0f);
        cb.restoreState();
    }

    // --------------------------------------------------------------- signature

    private void drawSignature(PdfContentByte cb, float w, CertificateEligibility config,
                               String instituteSignatory, String instituteSignatoryTitle) {
        float x = w - 300;
        float y = 190;
        String signatory = StringUtils.hasText(config.getSignatoryName())
                ? config.getSignatoryName()
                : (StringUtils.hasText(instituteSignatory) ? instituteSignatory : "Authorised Signatory");
        String designation = StringUtils.hasText(config.getSignatoryDesignation())
                ? config.getSignatoryDesignation()
                : (StringUtils.hasText(instituteSignatoryTitle) ? instituteSignatoryTitle : "Course Director");

        // scripted-looking signature above the rule
        text(cb, BaseFont.TIMES_BOLDITALIC, 24, x + 105, y + 8, Element.ALIGN_CENTER,
                signatory, ROYAL_BLUE, 0f, -3f);
        cb.saveState();
        cb.setColorStroke(DEEP_NAVY);
        cb.setLineWidth(1);
        cb.moveTo(x, y);
        cb.lineTo(x + 210, y);
        cb.stroke();
        cb.restoreState();
        text(cb, BaseFont.HELVETICA_BOLD, 9, x + 105, y - 14, Element.ALIGN_CENTER,
                signatory.toUpperCase(), DEEP_NAVY, 0.8f, 0f);
        text(cb, BaseFont.HELVETICA, 8, x + 105, y - 26, Element.ALIGN_CENTER,
                designation.toUpperCase(), INK_GRAY, 1f, 0f);
    }

    // ------------------------------------------------------------------ footer

    private void drawFooter(PdfContentByte cb, float w, String certificateNumber, String verificationUrl,
                            String certificateFooterNote) {
        float cx = w / 2f;
        text(cb, BaseFont.HELVETICA_BOLD, 10, cx, 110, Element.ALIGN_CENTER,
                "Certificate No. " + certificateNumber, DEEP_NAVY, 0.8f, 0f);
        text(cb, BaseFont.HELVETICA, 8.5f, cx, 94, Element.ALIGN_CENTER,
                "Verify this certificate at:  " + verificationUrl, INK_GRAY, 0.3f, 0f);
        text(cb, BaseFont.HELVETICA, 7, cx, 74, Element.ALIGN_CENTER,
                StringUtils.hasText(certificateFooterNote) ? certificateFooterNote
                        : ("This document certifies completion of the stated programme at Future Bound Tech. "
                        + "It is not an accredited qualification unless separately recognised in writing."),
                new Color(156, 163, 175), 0.2f, 0f);
    }

    // ----------------------------------------------------------------- helpers

    private void text(PdfContentByte cb, String baseFont, float size, float x, float y,
                      int alignment, String content, Color color, float characterSpacing, float rotation) {
        try {
            Font font = new Font(BaseFont.createFont(baseFont, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED),
                    size, Font.NORMAL, color);
            Chunk chunk = new Chunk(content, font);
            if (characterSpacing > 0) {
                chunk.setCharacterSpacing(characterSpacing);
            }
            Phrase phrase = new Phrase();
            phrase.add(chunk);
            ColumnText.showTextAligned(cb, alignment, phrase, x, y, rotation);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render text on certificate: " + content, ex);
        }
    }
}
