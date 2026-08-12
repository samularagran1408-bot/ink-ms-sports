package com.inklusport.sports.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * El QR de asistencia puede ser el código crudo ({@code QR_...}) o una URL
 * {@code https://.../asistencia?code=QR_...} para que la cámara del celular abra la encuesta.
 */
public final class QrCodeParser {

    private QrCodeParser() {
    }

    public static String extract(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        try {
            URI uri = URI.create(trimmed);
            String query = uri.getRawQuery();
            if (query != null && !query.isBlank()) {
                for (String part : query.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                    if ("code".equalsIgnoreCase(key)
                            || "qr".equalsIgnoreCase(key)
                            || "qrCode".equalsIgnoreCase(key)) {
                        String extracted = value.trim();
                        if (!extracted.isEmpty()) {
                            return extracted;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            /**
             * No era una URL; se usa el valor original.
             */
        }

        return trimmed;
    }
}
