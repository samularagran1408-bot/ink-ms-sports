package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resuelve identidades de usuario de forma canónica (UUID) y expone alias
 * (email / UUID) para lecturas compatibles con datos históricos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserIdentityService {

    private static final long CACHE_TTL_MS = 60_000L;

    private final UserServiceClient userServiceClient;
    private final ConcurrentHashMap<String, CacheEntry> idByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> emailById = new ConcurrentHashMap<>();

    /** Principal JWT actual o null si es anónimo/ausente. */
    public String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal == null) {
            return null;
        }
        String value = String.valueOf(principal).trim();
        if (value.isEmpty() || "anonymousUser".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    /**
     * Preferencia: UUID del request → UUID resuelto desde JWT email → email/principal → request.
     */
    public String resolveCanonicalUserId(String requested) {
        String principal = currentPrincipal();

        if (isUuidLike(requested)) {
            return requested.trim();
        }

        if (principal != null) {
            String fromEmail = resolveIdByEmail(principal);
            if (fromEmail != null) {
                return fromEmail;
            }
            if (isUuidLike(principal)) {
                return principal;
            }
        }

        if (requested != null && !requested.isBlank()) {
            if (requested.contains("@")) {
                String fromRequestedEmail = resolveIdByEmail(requested.trim());
                if (fromRequestedEmail != null) {
                    return fromRequestedEmail;
                }
            }
            return requested.trim();
        }

        if (principal != null) {
            return principal;
        }

        throw new IllegalArgumentException("No se pudo resolver el usuario autenticado.");
    }

    /**
     * Igual que {@link #resolveCanonicalUserId(String)} pero prioriza el trainerId
     * enviado por el cliente cuando el JWT no se puede resolver a UUID.
     */
    public String resolveTrainerId(String requested) {
        String principal = currentPrincipal();

        if (principal != null) {
            String fromEmail = resolveIdByEmail(principal);
            if (fromEmail != null) {
                return fromEmail;
            }
        }

        if (isUuidLike(requested)) {
            return requested.trim();
        }

        if (requested != null && !requested.isBlank()) {
            if (requested.contains("@")) {
                String fromRequestedEmail = resolveIdByEmail(requested.trim());
                if (fromRequestedEmail != null) {
                    return fromRequestedEmail;
                }
            }
            return requested.trim();
        }

        if (principal != null) {
            return principal;
        }

        throw new IllegalArgumentException("trainerId es obligatorio sin autenticación.");
    }

    /**
     * True si el usuario autenticado tiene alguno de los roles indicados, dados
     * sin el prefijo ROLE_ (el filtro JWT ya traduce ORGANIZADOR → ORGANIZER).
     */
    public boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Set<String> buscados = new LinkedHashSet<>();
        for (String rol : roles) {
            if (rol != null && !rol.isBlank()) {
                buscados.add(rol.trim().toUpperCase(Locale.ROOT));
            }
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority() == null ? "" : a.getAuthority().toUpperCase(Locale.ROOT))
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .anyMatch(buscados::contains);
    }

    /** Conjunto UUID/email del usuario para búsquedas históricas. */
    public Set<String> identityAliases(String userIdOrEmail) {
        Set<String> aliases = new LinkedHashSet<>();
        if (userIdOrEmail == null || userIdOrEmail.isBlank()) {
            return aliases;
        }

        String value = userIdOrEmail.trim();
        aliases.add(value);

        String principal = currentPrincipal();
        if (principal != null) {
            aliases.add(principal);
        }

        boolean hasUuid = aliases.stream().anyMatch(UserIdentityService::isUuidLike);
        boolean hasEmail = aliases.stream().anyMatch(a -> a != null && a.contains("@"));
        if (hasUuid && hasEmail) {
            return aliases;
        }

        if (value.contains("@")) {
            String id = resolveIdByEmail(value);
            if (id != null) {
                aliases.add(id);
            }
        } else {
            String email = resolveEmailById(value);
            if (email != null) {
                aliases.add(email);
            }
        }

        if (principal != null && principal.contains("@") && !principal.equals(value)) {
            String id = resolveIdByEmail(principal);
            if (id != null) {
                aliases.add(id);
            }
        }

        return aliases;
    }

    /** UUID desde email vía users-ms; null si no se resuelve. */
    private String resolveIdByEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String key = email.trim().toLowerCase();
        CacheEntry cached = idByEmail.get(key);
        if (cached != null && cached.valid()) {
            return cached.value();
        }
        try {
            Map<String, String> resolved = userServiceClient.getUserIdByEmail(email);
            if (resolved == null) {
                return null;
            }
            String id = resolved.get("id");
            if (id == null || id.isBlank() || "fallback-id".equalsIgnoreCase(id)) {
                return null;
            }
            String trimmed = id.trim();
            idByEmail.put(key, CacheEntry.of(trimmed, CACHE_TTL_MS));
            return trimmed;
        } catch (Exception e) {
            log.debug("No se resolvió UUID para {}: {}", email, e.getMessage());
            return null;
        }
    }

    /** Email desde UUID vía users-ms; null si no se resuelve. */
    private String resolveEmailById(String userId) {
        if (userId == null || userId.isBlank() || userId.contains("@")) {
            return null;
        }
        CacheEntry cached = emailById.get(userId);
        if (cached != null && cached.valid()) {
            return cached.value();
        }
        try {
            Map<String, Object> user = userServiceClient.getUserByIdInternal(userId);
            if (user == null || user.isEmpty()) {
                user = userServiceClient.getUserById(userId);
            }
            if (user == null) {
                return null;
            }
            Object email = user.get("email");
            if (email == null) {
                return null;
            }
            String value = String.valueOf(email).trim();
            if (!value.contains("@") || value.startsWith("no-disponible@")) {
                return null;
            }
            emailById.put(userId, CacheEntry.of(value, CACHE_TTL_MS));
            return value;
        } catch (Exception e) {
            log.debug("No se resolvió email para {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** True si el valor parece UUID (sin @ ni espacios, ≥32 chars). */
    private static boolean isUuidLike(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 32 && !trimmed.contains("@") && !trimmed.contains(" ");
    }

    private record CacheEntry(String value, long expiresAt) {
        static CacheEntry of(String value, long ttlMs) {
            return new CacheEntry(value, System.currentTimeMillis() + ttlMs);
        }

        boolean valid() {
            return value != null && System.currentTimeMillis() < expiresAt;
        }
    }
}
