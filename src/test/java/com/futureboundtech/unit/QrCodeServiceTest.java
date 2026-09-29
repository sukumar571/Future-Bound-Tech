package com.futureboundtech.unit;

import com.futureboundtech.service.QrCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the demo QR generator returns a self-contained PNG data URI. */
class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    @DisplayName("encodes a payload into a base-64 PNG data URI")
    void producesDataUri() {
        String uri = service.toPngDataUri("upi://pay?pa=futureboundtech@demo&am=45000.00&cu=INR", 220);
        assertNotNull(uri);
        assertTrue(uri.startsWith("data:image/png;base64,"));
        assertTrue(uri.length() > 100, "expected a non-trivial PNG payload");
    }

    @Test
    @DisplayName("returns null for blank input rather than an empty image")
    void blankReturnsNull() {
        assertNull(service.toPngDataUri("   ", 220));
        assertNull(service.toPngDataUri(null, 220));
    }
}
