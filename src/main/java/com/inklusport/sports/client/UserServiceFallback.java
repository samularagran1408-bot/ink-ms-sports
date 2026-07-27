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
        return fallback;
    }

    @Override
    public List<String> getUserRoles(String email) {
        log.warn(" Users MS no disponible. Rol por defecto para: {}", email);
        return List.of("USUARIO");
    }

    @Override
    public Map<String, String> getUserIdByEmail(String email) {
        log.warn(" Users MS no disponible. No se pudo obtener ID para: {}", email);
        Map<String, String> fallback = new HashMap<>();
        fallback.put("id", "fallback-id");
        fallback.put("email", email);
        return fallback;
    }
}