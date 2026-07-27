package com.inklusport.sports.controller;

import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.service.EventService;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.enums.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Endpoints para consulta y creacion de eventos deportivos.
 */
@RestController
@RequestMapping("/api/events")
@PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
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