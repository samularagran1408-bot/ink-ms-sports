package com.inklusport.sports.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/** */
@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {

    @Override
    public Map<String, Object> getUserById(String id) {
        log.warn(" Users MS no disponible. No se pudo obtener usuario con ID: {}", id);
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("id", id);
        fallback.put("fullName", "Usuario no disponible");
        fallback.put("email", "no-disponible@inklusport.com");
        fallback.put("trainerQuizPassed", false);
        return fallback;
    }

    @Override
    public Map<String, Object> getUserByIdInternal(String id) {
        return getUserById(id);
    }

    @Override
    public Map<String, Object> getVerificationStatus(String userId) {
        return getUserById(userId);
    }

    @Override
    public List<String> getUserRoles(String email) {
        log.warn(" Users MS no disponible. Rol por defecto para: {}", email);
        return List.of("USUARIO");
    }

    @Override
    public Map<String, String> getUserIdByEmail(String email) {
        log.warn("Users MS no disponible. No se pudo obtener ID para: {}", email);
        // No inventar un UUID falso: eso hacía que rutinas/eventos quedaran
        // asociados a "fallback-id" y desaparecieran al listar por el id real.
        return new HashMap<>();
    }
}