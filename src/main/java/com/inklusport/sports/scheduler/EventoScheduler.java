package com.inklusport.sports.scheduler;

import com.inklusport.sports.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoScheduler {

    private final EventService eventService;

    /**
     * Procesa los estados de eventos cada 10 segundos
     * 1. DRAFT → ACTIVE (cuando llega la fecha y hora)
     * 2. ACTIVE → FINISHED (2 horas después del evento)
     * 3. FINISHED → eliminado (24 horas después de finalizar)
     */
    @Scheduled(cron = "*/10 * * * * *", zone = "America/Bogota")
    public void procesarEstadosEventos() {
        log.info("[Scheduler] Procesando estados de eventos");
        
        try {
            eventService.procesarEstadosEventos();
        } catch (Exception e) {
            log.error("[Scheduler] Error procesando eventos: {}", e.getMessage(), e);
        }
    }
}