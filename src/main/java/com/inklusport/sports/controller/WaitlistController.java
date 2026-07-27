package com.inklusport.sports.controller;

import com.inklusport.sports.dto.WaitlistRequest;
import com.inklusport.sports.dto.WaitlistResponse;
import com.inklusport.sports.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para gestionar la lista de espera de eventos.
 */
@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    /**
     * Obtiene la waitlist completa de un evento.
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<WaitlistResponse>> getWaitlistByEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(waitlistService.getWaitlistByEvent(eventId));
    }

    /**
     * Agrega un usuario a la waitlist.
     */
    @PostMapping
    public ResponseEntity<WaitlistResponse> addToWaitlist(@RequestBody WaitlistRequest request) {
        return ResponseEntity.ok(waitlistService.addToWaitlist(request));
    }
}