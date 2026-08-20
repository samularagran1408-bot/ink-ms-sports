package com.inklusport.sports.controller;

import com.inklusport.sports.dto.SportDisabilityRequest;
import com.inklusport.sports.dto.SportDisabilityResponse;
import com.inklusport.sports.service.SportDisabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestiona las adaptaciones entre deporte y discapacidad.
 */
@RestController
@RequestMapping("/api/sport-disabilities")
@RequiredArgsConstructor
public class SportDisabilityController {

    private final SportDisabilityService sportDisabilityService;

    /**
     * Lista adaptaciones asociadas a un deporte.
     */
    @GetMapping("/sport/{sportId}")
    public ResponseEntity<List<SportDisabilityResponse>> getSportDisabilities(@PathVariable Integer sportId) {
        return ResponseEntity.ok(sportDisabilityService.getSportDisabilities(sportId));
    }

    /**
     * Lista asociaciones con discapacidades activas.
     */
    @GetMapping
    public ResponseEntity<List<SportDisabilityResponse>> getAllAssociations() {
        return ResponseEntity.ok(sportDisabilityService.getAllAssociations());
    }

    /**
     * Busca asociaciones (deporte, discapacidad o adaptaciones). Solo discapacidades activas.
     */
    @GetMapping("/search")
    public ResponseEntity<List<SportDisabilityResponse>> searchAssociations(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(sportDisabilityService.searchAssociations(q));
    }

    /**
     * Crea una adaptacion deporte-discapacidad.
     */
    @PostMapping
    public ResponseEntity<SportDisabilityResponse> addAdaptation(@RequestBody SportDisabilityRequest request) {
        return ResponseEntity.ok(sportDisabilityService.addAdaptation(request));
    }

    /**
     * Actualiza una adaptacion existente.
     */
    @PutMapping("/sport/{sportId}/disability/{disabilityId}")
    public ResponseEntity<SportDisabilityResponse> updateAdaptation(
            @PathVariable Integer sportId,
            @PathVariable Integer disabilityId,
            @RequestBody SportDisabilityRequest request) {
        return ResponseEntity.ok(sportDisabilityService.updateAdaptation(sportId, disabilityId, request));
    }

    /**
     * Elimina una adaptacion de un deporte/discapacidad.
     */
    @DeleteMapping("/sport/{sportId}/disability/{disabilityId}")
    public ResponseEntity<Void> removeAdaptation(
            @PathVariable Integer sportId,
            @PathVariable Integer disabilityId) {
        sportDisabilityService.removeAdaptation(sportId, disabilityId);
        return ResponseEntity.noContent().build();
    }
}