package com.inklusport.sports.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PruebaScheduler {

    /**
     * Tarea de prueba cada 30 segundos
     * Después de confirmar que funciona, puedes comentar o eliminar este scheduler
     */
    @Scheduled(fixedDelay = 30000)
    public void pruebaScheduler() {
        log.info("[Scheduler - TEST] El scheduler está funcionando a las {}", LocalDateTime.now());
    }
}