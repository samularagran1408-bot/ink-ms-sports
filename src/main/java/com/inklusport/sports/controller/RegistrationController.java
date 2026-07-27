package com.inklusport.sports.controller;

import com.inklusport.sports.dto.RegistrationRequest;
import com.inklusport.sports.dto.RegistrationResponse;
import com.inklusport.sports.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.List;

/**
 * Endpoints de inscripcion y cancelacion para eventos.
 */
@RestController
@RequestMapping("/api/registrations")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Inscribe a un usuario en un evento.
     * Puede devolver respuesta de exito o un objeto de error de negocio.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registerToEvent(@RequestBody RegistrationRequest request) { // 🌟 Cambiado a <?> para soportar respuestas de error mixtas
        try {
            RegistrationResponse response = registrationService.registerToEvent(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FATAL_ERROR",
                    "message", "Error inesperado: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancela una inscripcion y ajusta la lista de espera si aplica.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRegistration(@PathVariable String id) {
        try {
            registrationService.cancelRegistration(id);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Inscripción cancelada correctamente y lista de espera actualizada."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Inscripciones de un usuario (consumo interno / AI assistant).
     * Debe ir antes de {eventId}/waitlist para evitar ambigüedad de rutas.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RegistrationResponse>> getRegistrationsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByUser(userId));
    }

    /**
     * Consulta la waitlist asociada a un evento.
     */
    @GetMapping("/{eventId}/waitlist")
    public ResponseEntity<List<RegistrationResponse>> getWaitlist(@PathVariable String eventId) {
        return ResponseEntity.ok(registrationService.getWaitlistForEvent(eventId));
    }
}