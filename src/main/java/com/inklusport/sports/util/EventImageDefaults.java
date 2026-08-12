package com.inklusport.sports.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Fotos de portada predeterminadas por deporte (rutas servidas por el frontend).
 */
public final class EventImageDefaults {

    public static final String DEFAULT_IMAGE = "assets/events/default.png";

    private EventImageDefaults() {}

    public static String forSport(String sportName) {
        if (sportName == null || sportName.isBlank()) {
            return DEFAULT_IMAGE;
        }
        String key = normalize(sportName);
        if (key.contains("futbol") || key.contains("football") || key.contains("soccer")) {
            return "assets/events/futbol-sala.png";
        }
        if (key.contains("baloncesto") || key.contains("basket")) {
            return "assets/events/baloncesto-silla.png";
        }
        if (key.contains("natacion") || key.contains("swim")) {
            return "assets/events/natacion.png";
        }
        return DEFAULT_IMAGE;
    }

    private static String normalize(String value) {
        String nfd = Normalizer.normalize(value, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
