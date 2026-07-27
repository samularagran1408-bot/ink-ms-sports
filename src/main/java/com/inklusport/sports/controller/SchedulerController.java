package com.inklusport.sports.controller;

import com.inklusport.sports.service.EventReminderService;
import com.inklusport.sports.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@Slf4j
public class SchedulerController {

    private final EventReminderService reminderService;
    private final EventService eventService;

    /**
     * Endpoint para probar manualmente la transición de estados de eventos.
     * POST /api/scheduler/process-events
     */
    @PostMapping("/process-events")
    public ResponseEntity<?> processEvents() {
        log.info("Ejecutando procesamiento de estados de eventos manualmente");
        eventService.procesarEstadosEventos();
        return ResponseEntity.ok(Map.of(
            "message", "Estados de eventos procesados",
            "status", "success"
        ));
    }

    /**
     * Endpoint para probar manualmente el envío de recordatorios
     * POST /api/scheduler/test-reminders
     */
    @PostMapping("/test-reminders")
    public ResponseEntity<?> testReminders() {
        log.info("Ejecutando envío de recordatorios manualmente");
        
        try {
            reminderService.sendEventReminders();
            return ResponseEntity.ok(Map.of(
                "message", "Recordatorios enviados exitosamente",
                "status", "success"
            ));
        } catch (Exception e) {
            log.error("Error al enviar recordatorios: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Error al enviar recordatorios: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
}