package com.inklusport.sports.controller;

import com.inklusport.sports.dto.DisabilityRequest;
import com.inklusport.sports.dto.DisabilityResponse;
import com.inklusport.sports.service.DisabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Endpoints CRUD del catalogo de discapacidades.
 */
@RestController
@RequestMapping("/api/disabilities")
@PreAuthorize("hasRole('ADMIN') or hasRole('COACH') or hasRole('ORGANIZER')")
@RequiredArgsConstructor
public class DisabilityController {

    private final DisabilityService disabilityService;

    /**
     * Lista todas las discapacidades.
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DisabilityResponse>> getAllDisabilities() {
        return ResponseEntity.ok(disabilityService.getAllDisabilities());
    }

    /**
     * Lista solo discapacidades activas.
     */
    @GetMapping("/active")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DisabilityResponse>> getActiveDisabilities() {
        return ResponseEntity.ok(disabilityService.getActiveDisabilities());
    }

    /**
     * Obtiene una discapacidad por id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DisabilityResponse> getDisabilityById(@PathVariable Integer id) {
        return ResponseEntity.ok(disabilityService.getDisabilityById(id));
    }

    /**
     * Crea una discapacidad.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<DisabilityResponse> createDisability(@RequestBody DisabilityRequest request) {
        return ResponseEntity.ok(disabilityService.createDisability(request));
    }

    /**
     * Actualiza una discapacidad existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<DisabilityResponse> updateDisability(@PathVariable Integer id, @RequestBody DisabilityRequest request) {
        return ResponseEntity.ok(disabilityService.updateDisability(id, request));
    }

    /**
     * Elimina una discapacidad por id.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<Void> deleteDisability(@PathVariable Integer id) {
        disabilityService.deleteDisability(id);
        return ResponseEntity.noContent().build();
    }
}