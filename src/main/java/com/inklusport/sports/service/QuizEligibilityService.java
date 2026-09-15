package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El quiz aprobado es el único requisito operativo para entrenador/organizador.
 * ADMIN y peticiones sin autenticación (perfil docker) quedan exentos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizEligibilityService {

    private static final long CACHE_OK_MS = 60_000L;
    private static final long CACHE_PENDING_MS = 5_000L;

    private final UserServiceClient userServiceClient;
    private final UserIdentityService userIdentityService;
    private final ConcurrentHashMap<String, QuizCache> cache = new ConcurrentHashMap<>();

    /**
     * Exige trainerQuizPassed antes de gestionar rutinas.
     */
    public void assertTrainerQuizPassed(String trainerId) {
        if (isAdmin() || !isAuthenticated()) {
            return;
        }
        String userId = resolveUserId(trainerId);
        QuizCache cached = cache.get(userId);
        if (cached != null && cached.valid() && cached.trainer()) {
            return;
        }
        QuizCache flags = flagsOf(userId);
        if (!flags.trainer()) {
            throw new IllegalStateException(
                    "Debes completar el quiz de aptitud de entrenador antes de gestionar rutinas o atletas."
            );
        }
    }

    /**
     * Exige organizerQuizPassed antes de gestionar eventos.
     */
    public void assertOrganizerQuizPassed(String organizerId) {
        if (isAdmin() || !isAuthenticated()) {
            return;
        }
        String userId = resolveUserId(organizerId);
        QuizCache cached = cache.get(userId);
        if (cached != null && cached.valid() && cached.organizer()) {
            return;
        }
        QuizCache flags = flagsOf(userId);
        if (!flags.organizer()) {
            throw new IllegalStateException(
                    "Debes completar el quiz de aptitud de organizador antes de gestionar eventos o inscritos."
            );
        }
    }

    /**
     * Para asistencia/inscritos: basta quiz de organizador o de entrenador.
     */
    public void assertCurrentStaffQuizPassed() {
        if (isAdmin() || !isAuthenticated()) {
            return;
        }
        String principal = userIdentityService.currentPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Debes autenticarte para continuar.");
        }
        String userId = resolveUserId(principal);
        QuizCache cached = cache.get(userId);
        if (cached != null && cached.valid() && (cached.organizer() || cached.trainer())) {
            return;
        }
        QuizCache flags = flagsOf(userId);
        if (!flags.organizer() && !flags.trainer()) {
            throw new IllegalStateException(
                    "Debes completar el quiz de aptitud antes de gestionar inscritos o asistencia."
            );
        }
    }

    /**
     * Indica si el JWT actual tiene rol ADMIN.
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Indica si hay un principal autenticado en el contexto de seguridad.
     */
    private boolean isAuthenticated() {
        return userIdentityService.currentPrincipal() != null;
    }

    /**
     * Resuelve email/UUID a un identificador canónico de usuario.
     */
    private String resolveUserId(String raw) {
        try {
            return userIdentityService.resolveCanonicalUserId(raw);
        } catch (Exception e) {
            return raw;
        }
    }

    private QuizCache flagsOf(String userId) {
        Map<String, Object> user = loadProfile(userId);
        boolean trainer = boolFlag(user, "trainerQuizPassed") || boolFlag(user, "trainer_quiz_passed");
        boolean organizer = boolFlag(user, "organizerQuizPassed") || boolFlag(user, "organizer_quiz_passed");
        long ttl = (trainer || organizer) ? CACHE_OK_MS : CACHE_PENDING_MS;
        QuizCache flags = new QuizCache(trainer, organizer, System.currentTimeMillis() + ttl);
        if (userId != null && !userId.isBlank()) {
            cache.put(userId, flags);
        }
        return flags;
    }

    /**
     * Carga el perfil/estado de verificación desde users-ms con varios fallbacks.
     */
    private Map<String, Object> loadProfile(String userId) {
        Map<String, Object> user = null;
        try {
            user = userServiceClient.getVerificationStatus(userId);
        } catch (Exception e) {
            log.debug("verify/status falló para {}: {}", userId, e.getMessage());
        }
        if (user == null || user.isEmpty()) {
            try {
                user = userServiceClient.getUserByIdInternal(userId);
            } catch (Exception e) {
                log.debug("internal user falló para {}: {}", userId, e.getMessage());
            }
        }
        if (user == null || user.isEmpty()) {
            try {
                user = userServiceClient.getUserById(userId);
            } catch (Exception e) {
                log.debug("getUserById falló para {}: {}", userId, e.getMessage());
            }
        }
        return user != null ? user : Map.of();
    }

    /**
     * Interpreta flags booleanos que pueden llegar como Boolean, String o Number.
     */
    private static boolean boolFlag(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
    }

    private record QuizCache(boolean trainer, boolean organizer, long expiresAt) {
        boolean valid() {
            return System.currentTimeMillis() < expiresAt;
        }
    }
}
