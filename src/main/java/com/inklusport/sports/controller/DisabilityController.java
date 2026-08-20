package com.inklusport.sports.controller;

import com.inklusport.sports.dto.DisabilityRequest;
import com.inklusport.sports.dto.DisabilityResponse;
import com.inklusport.sports.service.DisabilityService;
import jakarta.validation.Valid;
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
     * Lista todas las discapacidades (activas e inactivas).
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
     * Busca discapacidades activas por nombre o ID. Las desactivadas no aparecen.
     */
    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DisabilityResponse>> searchDisabilities(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(disabilityService.searchActiveDisabilities(q));
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
    public ResponseEntity<DisabilityResponse> createDisability(@Valid @RequestBody DisabilityRequest request) {
        return ResponseEntity.ok(disabilityService.createDisability(request));
    }

    /**
     * Actualiza una discapacidad existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<DisabilityResponse> updateDisability(
            @PathVariable Integer id,
            @Valid @RequestBody DisabilityRequest request) {
        return ResponseEntity.ok(disabilityService.updateDisability(id, request));
    }

    /**
     * Desactiva una discapacidad (soft-delete). Falla si ya está desactivada.
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<DisabilityResponse> deactivateDisability(@PathVariable Integer id) {
        return ResponseEntity.ok(disabilityService.deactivateDisability(id));
    }

    /**
     * Reactiva una discapacidad previamente desactivada.
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<DisabilityResponse> activateDisability(@PathVariable Integer id) {
        return ResponseEntity.ok(disabilityService.activateDisability(id));
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
