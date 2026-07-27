package com.inklusport.sports.scheduler;

import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimpiezaScheduler {

    private final EventAttendanceRepository attendanceRepository;
    private final EventRegistrationRepository registrationRepository;

    /**
     * Elimina registros antiguos todos los domingos a las 3 AM
     */
    @Scheduled(cron = "0 0 3 * * 0", zone = "America/Bogota")
    public void limpiarRegistrosAntiguos() {
        log.info("[Scheduler] Limpiando registros antiguos");
        
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(6);
            
            long asistenciasEliminadas = attendanceRepository.deleteByCheckInTimeBefore(fechaLimite);
            long registrosEliminados = registrationRepository.deleteByRegistrationDateBefore(fechaLimite);
            
            log.info("[Scheduler] Asistencias eliminadas: {}, Registros eliminados: {}", 
                asistenciasEliminadas, registrosEliminados);
        } catch (Exception e) {
            log.error("[Scheduler] Error limpiando registros: {}", e.getMessage());
        }
    }
}