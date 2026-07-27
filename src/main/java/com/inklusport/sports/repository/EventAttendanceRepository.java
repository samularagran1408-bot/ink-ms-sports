package com.inklusport.sports.repository;

import com.inklusport.sports.entity.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, String> {

    /**
     * Verifica si existe una asistencia por ID
     */
    boolean existsByRegistrationId(String registrationId);

    /**
     * Busca asistencias por ID
     */
    List<EventAttendance> findByRegistrationId(String registrationId);

    /**
     * Busca asistencias para un evento
     */
    List<EventAttendance> findByRegistration_EventId(String eventId);

    /**
     * Elimina asistencias anteriores a la fecha
     */
    long deleteByCheckInTimeBefore(LocalDateTime fecha);

    /**
     * Cuenta asistencias anteriores a la fecha
     */
    long countByCheckInTimeBefore(LocalDateTime fecha);

    /**
     * 
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM EventAttendance a WHERE a.checkInTime < :fechaLimite")
    int deleteByCheckInTimeBeforeWithQuery(@Param("fechaLimite") LocalDateTime fechaLimite);

}