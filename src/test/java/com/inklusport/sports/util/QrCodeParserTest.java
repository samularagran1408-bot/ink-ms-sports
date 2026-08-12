package com.inklusport.sports.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QrCodeParserTest {

    @Test
    void keepsRawQrCode() {
        assertEquals("QR_abc-123", QrCodeParser.extract("QR_abc-123"));
    }

    @Test
    void extractsCodeFromAttendanceUrl() {
        String url = "https://inklusport.inklusport.uk/asistencia?code=QR_abc-123&eventId=evt-1";
        assertEquals("QR_abc-123", QrCodeParser.extract(url));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(QrCodeParser.extract(null));
    }
}
