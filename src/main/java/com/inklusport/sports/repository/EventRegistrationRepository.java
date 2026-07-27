package com.inklusport.sports.repository;

import com.inklusport.sports.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
    
    List<EventRegistration> findByUserId(String userId);
    
    List<EventRegistration> findByEventId(String eventId);

    List<EventRegistration> findByEventIdAndWaitlistPositionIsNull(String eventId);

    List<EventRegistration> findByEventIdAndWaitlistPositionIsNullAndReminderSentAtIsNull(String eventId);
    
    Optional<EventRegistration> findByUserIdAndEventId(String userId, String eventId);
    
    boolean existsByEventIdAndUserId(String eventId, String userId);
    
    /**
     * Cuenta cuántos usuarios están en lista de espera (donde waitlist_position NO es nulo)
     */
    long countByEventIdAndWaitlistPositionIsNotNull(String eventId);

    /**
     * Busca al primero de la fila: el que tenga la posición más baja (ej: posición 1)
     */
    Optional<EventRegistration> findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(String eventId);

    /**
     * Trae a todos los que están en la fila para poder reordenarlos cuando uno se salga
     */
    List<EventRegistration> findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(String eventId);

    /**
     * Cambia la posición de un usuario en la lista de espera
     */
    @Modifying
    @Transactional
    @Query("UPDATE EventRegistration er SET er.waitlistPosition = NULL WHERE er.userId = :userId AND er.eventId = :eventId")
    void updateWaitlistToConfirmed(@Param("userId") String userId, @Param("eventId") String eventId);

    /**
     * Elimina registros anteriores a la fecha
     */
    long deleteByRegistrationDateBefore(LocalDateTime fecha);

    /**
     * Cuenta registros anteriores a la fecha
     */
    long countByRegistrationDateBefore(LocalDateTime fecha);

    /**
     * Elimina registros antiguos con un evento finalizado
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM EventRegistration r WHERE r.eventId IN (SELECT e.id FROM Event e WHERE e.status = 'finished')")
    int deleteByEventFinished();
}