package com.inklusport.sports.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Consultas puntuales al asistente de IA, dueño del modo competencia (RF53).
 */
@FeignClient(
        name = "ink-ms-ai-assistant",
        url = "${ai.service.url:http://localhost:3008}",
        configuration = AiAssistantClientConfig.class
)
public interface AiAssistantClient {

    /**
     * Estado del modo competencia de uno o varios usuarios (ids o correos
     * separados por coma). Responde {@code {usuarios: {id: bool}, activo: bool}}.
     */
    @GetMapping("/api/ai/competencia/modo-activo")
    Map<String, Object> competitionMode(@RequestParam("usuarios") String usuarios);

    /**
     * Suma al plan de competencia del atleta la asistencia confirmada a un evento.
     * El token viaja explícito porque la llamada sale fuera del hilo de la petición.
     */
    @PostMapping("/api/ai/competencia/evento-asistido")
    Map<String, Object> registerEventAttendance(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Map<String, Object> body
    );
}
