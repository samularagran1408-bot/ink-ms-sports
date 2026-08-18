package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Event;
import com.inklusport.sports.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByStatus(EventStatus status);

    List<Event> findByStatusOrderByEventDateAscEventTimeAsc(EventStatus status);

    List<Event> findByEventDateAndStatus(LocalDate eventDate, EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = :status " +
           "AND (:fromDate IS NULL OR e.eventDate >= :fromDate) " +
           "AND (:toDate IS NULL OR e.eventDate <= :toDate) " +
           "ORDER BY e.eventDate ASC, e.eventTime ASC")
    List<Event> findCalendarEvents(
            @Param("status") EventStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    long countByStatus(EventStatus status);

    long countBySportIdAndStatus(Long sportId, EventStatus status);

    @Query("SELECT e FROM Event e WHERE " +
           "e.status = :draftStatus AND " +
           "(e.eventDate < :hoy OR (e.eventDate = :hoy AND e.eventTime <= :ahora))")
    List<Event> findDraftEventsToActivate(
        @Param("draftStatus") EventStatus draftStatus,
        @Param("hoy") LocalDate hoy,
        @Param("ahora") LocalTime ahora
    );

    @Query("SELECT e FROM Event e WHERE " +
           "e.status = :activeStatus AND " +
           "(e.eventDate < :hoy OR (e.eventDate = :hoy AND e.eventTime <= :horaLimite))")
    List<Event> findActiveEventsToFinish(
        @Param("activeStatus") EventStatus activeStatus,
        @Param("hoy") LocalDate hoy,
        @Param("horaLimite") LocalTime horaLimite
    );

    @Modifying
    @Transactional
    @Query("UPDATE Event e SET e.status = :nuevoStatus WHERE " +
           "e.status = :draftStatus AND " +
           "(e.eventDate < :hoy OR (e.eventDate = :hoy AND e.eventTime <= :ahora))")
    int updateDraftToActive(
        @Param("draftStatus") EventStatus draftStatus,
        @Param("nuevoStatus") EventStatus nuevoStatus,
        @Param("hoy") LocalDate hoy,
        @Param("ahora") LocalTime ahora
    );

    @Modifying
    @Transactional
    @Query("UPDATE Event e SET e.status = :nuevoStatus WHERE " +
           "e.status = :activeStatus AND " +
           "(e.eventDate < :hoy OR (e.eventDate = :hoy AND e.eventTime <= :horaLimite))")
    int updateActiveToFinished(
        @Param("activeStatus") EventStatus activeStatus,
        @Param("nuevoStatus") EventStatus nuevoStatus,
        @Param("hoy") LocalDate hoy,
        @Param("horaLimite") LocalTime horaLimite
    );
}