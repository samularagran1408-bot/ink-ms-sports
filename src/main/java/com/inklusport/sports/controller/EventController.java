package com.inklusport.sports.controller;

import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.dto.EventUpdateRequest;
import com.inklusport.sports.service.EventService;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.enums.EventStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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
     * Lista los eventos disponibles.
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * Crea un evento nuevo.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    /**
     * Actualiza un evento (fecha, lugar, etc.). Notifica a inscritos si cambian fecha/lugar.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('ORGANIZADOR') or hasRole('COACH') or hasRole('ENTRENADOR')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
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
}
