package com.inklusport.sports.controller;

import com.inklusport.sports.dto.CalendarEventResponse;
import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.dto.EventUpdateRequest;
import com.inklusport.sports.service.EventService;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.enums.EventStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints para consulta, creación y actualización de eventos deportivos.
 */
@RestController
@RequestMapping("/api/events")
@PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;

    /**
     * Lista los eventos (gestión completa).
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * Lista eventos activos disponibles para inscripción.
     */
    @GetMapping("/available")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventResponse>> getAvailableEvents() {
        return ResponseEntity.ok(eventService.getAvailableEvents());
    }

    /**
     * Calendario de eventos activos. Si from/to no traen coincidencias, lista vacía.
     */
    @GetMapping("/calendar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CalendarEventResponse>> getCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(eventService.getCalendar(from, to));
    }

    /**
     * Busca eventos activos por texto y rango de fechas.
     */
    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(eventService.searchEvents(q, from, to));
    }

    @GetMapping("/active/count")
    public ResponseEntity<Long> countActiveEvents() {
        long count = eventRepository.countByStatus(EventStatus.active);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/active/count/by-sport/{sportId}")
    public ResponseEntity<Long> countActiveEventsBySport(@PathVariable Long sportId) {
        long count = eventRepository.countBySportIdAndStatus(sportId, EventStatus.active);
        return ResponseEntity.ok(count);
    }

    /**
     * Consulta un evento por id. 404 si no existe.
     */
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * Crea un evento nuevo.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    /**
     * Actualiza un evento (fecha, lugar, cupo, etc.). Notifica a inscritos si cambian fecha/lugar.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    /**
     * Cancela un evento activo o en borrador. Falla si ya está cancelado.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable String id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }
}
