package com.inklusport.sports.controller;

import com.inklusport.sports.dto.SportRequest;
import com.inklusport.sports.dto.SportResponse;
import com.inklusport.sports.service.SportService;
import com.inklusport.sports.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Endpoints CRUD para el catalogo de deportes.
 */
@RestController
@RequestMapping("/api/sports")
@PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;
    private final SportRepository sportRepository;

    /**
     * Lista todos los deportes.
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<SportResponse>> getAllSports() {
        return ResponseEntity.ok(sportService.getAllSports());
    }

    /**
     * Lista deportes activos.
     */
    @GetMapping("/active")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<SportResponse>> getActiveSports() {
        return ResponseEntity.ok(sportService.getActiveSports());
    }

    /**
     * Obtiene el detalle de un deporte por id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SportResponse> getSportById(@PathVariable Integer id) {
        return ResponseEntity.ok(sportService.getSportById(id));
    }

    /**
     * Crea un deporte nuevo.
     */
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<SportResponse> createSport(@RequestBody SportRequest request) {
        return ResponseEntity.ok(sportService.createSport(request));
    }

    /**
     * Actualiza un deporte existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SportResponse> updateSport(@PathVariable Integer id, @RequestBody SportRequest request) {
        return ResponseEntity.ok(sportService.updateSport(id, request));
    }

    /**
     * Elimina un deporte por id.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> deleteSport(@PathVariable Integer id) {
        sportService.deleteSport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countSports() {
        long count = sportRepository.count();
        return ResponseEntity.ok(count);
    }
}