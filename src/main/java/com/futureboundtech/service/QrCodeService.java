package com.futureboundtech.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a scannable QR code as a self-contained {@code data:image/png;base64,...}
 * URI so it can be embedded directly in a Thymeleaf page with no extra request or
 * external image service. Used for the optional demo UPI payment QR; it never
 * touches real payment processing.
 */
@Service
@Slf4j
public class QrCodeService {

    /**
     * @param text the payload to encode (e.g. a {@code upi://pay?...} string)
     * @param size square edge length in pixels
     * @return a base-64 PNG data URI, or {@code null} if encoding fails
     */
    public String toPngDataUri(String text, int size) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int edge = Math.max(80, Math.min(size, 1024));
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, edge, edge, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Throwable ex) {
            // Presentation-only: swallow encoding failures (including a LinkageError such as
            // NoClassDefFoundError if zxing is absent) so the page still renders without the QR.
            log.warn("Could not generate QR code: {}", ex.getMessage());
            return null;
        }
    }
}
