package com.inklusport.sports.service;

import com.inklusport.sports.client.AiAssistantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * El modo competencia vive en ink-ms-ai-assistant (RF53). Aquí sólo se consulta
 * para decidir si el entrenador puede registrarle asistencia a un atleta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionModeService {

    /** Activo se cachea más: desactivarlo es raro y la consulta no es gratis. */
    private static final long CACHE_ACTIVO_MS = 30_000L;
    /** Inactivo caduca antes para que el atleta pueda activarlo y seguir al momento. */
    private static final long CACHE_INACTIVO_MS = 5_000L;

    private final AiAssistantClient aiAssistantClient;
    private final ConcurrentHashMap<String, ModeCache> cache = new ConcurrentHashMap<>();

    /**
     * Exige modo competencia activo antes de marcar asistencia.
     *
     * @throws IllegalStateException si está desactivado o no se pudo verificar.
     */
    public void assertActive(String userId, String athleteEmail, String athleteName) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("No se pudo identificar al atleta de la inscripción.");
        }
        String nombre = (athleteName == null || athleteName.isBlank()) ? "El atleta" : athleteName;

        Boolean activo = isActive(userId, athleteEmail);
        if (activo == null) {
            throw new IllegalStateException(
                    "No se pudo verificar el modo competencia de " + nombre + ". Inténtalo de nuevo en unos segundos."
            );
        }
        if (!activo) {
            throw new IllegalStateException(
                    nombre + " debe tener el modo competencia activo para registrarle asistencia."
            );
        }
    }

    /**
     * Suma la asistencia a un evento al plan de competencia del atleta.
     *
     * <p>Va fuera del hilo del check-in: el progreso es un efecto secundario y
     * nunca debe retrasar ni tumbar el registro de asistencia. Si el atleta no
     * tiene el modo competencia activo, el asistente responde que no aplica.
     */
    @Async("aiTaskExecutor")
    public void registerEventProgress(
            String userId,
            String athleteEmail,
            String eventId,
            String eventName,
            String authorization
    ) {
        if (userId == null || userId.isBlank() || eventId == null || eventId.isBlank()) {
            return;
        }
        if (authorization == null || authorization.isBlank()) {
            log.debug("Sin token para sumar el progreso del evento {} a {}", eventId, userId);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usuario_id", userId.trim());
        body.put("evento_id", eventId.trim());
        if (athleteEmail != null && !athleteEmail.isBlank()) {
            body.put("email", athleteEmail.trim());
        }
        if (eventName != null && !eventName.isBlank()) {
            body.put("evento_nombre", eventName.trim());
        }
        try {
            Map<String, Object> respuesta = aiAssistantClient.registerEventAttendance(authorization, body);
            if (respuesta != null && Boolean.TRUE.equals(respuesta.get("registrado"))) {
                log.info(
                        "Asistencia al evento {} sumada al plan de {}: {}% del plan",
                        eventId, userId, respuesta.get("plan_pct")
                );
            }
        } catch (Exception e) {
            log.warn("No se pudo sumar el progreso del evento {} a {}: {}", eventId, userId, e.getMessage());
        }
    }

    /**
     * True/False según el asistente; null si no se pudo consultar.
     */
    public Boolean isActive(String userId, String athleteEmail) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String clave = userId.trim();
        ModeCache cached = cache.get(clave);
        if (cached != null && cached.valid()) {
            return cached.active();
        }

        Set<String> aliases = aliasesOf(clave, athleteEmail);
        String parametro = String.join(",", aliases);
        try {
            Map<String, Object> respuesta = aiAssistantClient.competitionMode(parametro);
            boolean activo = readActive(respuesta, aliases);
            cache.put(clave, ModeCache.of(activo, activo ? CACHE_ACTIVO_MS : CACHE_INACTIVO_MS));
            return activo;
        } catch (Exception e) {
            log.warn("No se pudo consultar el modo competencia de {}: {}", clave, e.getMessage());
            return null;
        }
    }

    /**
     * El documento del asistente se guarda por id o por correo, así que se
     * consultan los dos del atleta. Nunca se mezclan con los de quien pregunta.
     */
    private Set<String> aliasesOf(String userId, String athleteEmail) {
        return Stream.of(userId, athleteEmail)
                .filter(a -> a != null && !a.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Lee {@code usuarios} del cuerpo; cualquier alias activo vale.
     */
    @SuppressWarnings("unchecked")
    private boolean readActive(Map<String, Object> respuesta, Collection<String> aliases) {
        if (respuesta == null || respuesta.isEmpty()) {
            return false;
        }
        Object usuarios = respuesta.get("usuarios");
        if (usuarios instanceof Map<?, ?> mapa) {
            for (String alias : aliases) {
                if (Boolean.TRUE.equals(((Map<String, Object>) mapa).get(alias))) {
                    return true;
                }
            }
            return false;
        }
        return Boolean.TRUE.equals(respuesta.get("activo"));
    }

    private record ModeCache(boolean active, long expiresAt) {
        static ModeCache of(boolean active, long ttlMs) {
            return new ModeCache(active, System.currentTimeMillis() + ttlMs);
        }

        boolean valid() {
            return System.currentTimeMillis() < expiresAt;
        }
    }
}
