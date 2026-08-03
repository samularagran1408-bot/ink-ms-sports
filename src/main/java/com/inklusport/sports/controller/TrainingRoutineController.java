package com.inklusport.sports.controller;

import com.inklusport.sports.dto.RoutineRegistrationResponse;
import com.inklusport.sports.dto.RoutineRequest;
import com.inklusport.sports.dto.RoutineResponse;
import com.inklusport.sports.service.RoutineRegistrationService;
import com.inklusport.sports.service.TrainingRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class TrainingRoutineController {

    private final TrainingRoutineService routineService;
    private final RoutineRegistrationService registrationService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RoutineResponse>> listPublished() {
        return ResponseEntity.ok(routineService.listPublished());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(routineService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/trainer/{trainerId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RoutineResponse>> byTrainer(@PathVariable String trainerId) {
        return ResponseEntity.ok(routineService.listByTrainer(trainerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('COACH') or hasRole('ADMIN') or hasRole('ENTRENADOR')")
    public ResponseEntity<?> create(@Valid @RequestBody RoutineRequest request) {
        try {
            return ResponseEntity.ok(routineService.create(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COACH') or hasRole('ADMIN') or hasRole('ENTRENADOR')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody RoutineRequest request) {
        try {
            return ResponseEntity.ok(routineService.update(id, request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('COACH') or hasRole('ADMIN') or hasRole('ENTRENADOR')")
    public ResponseEntity<?> publish(@PathVariable String id) {
        try {
            return ResponseEntity.ok(routineService.publish(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("status", "FORBIDDEN", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasRole('COACH') or hasRole('ADMIN') or hasRole('ENTRENADOR')")
    public ResponseEntity<?> registrations(@PathVariable String id) {
        try {
            List<RoutineRegistrationResponse> list = registrationService.byRoutine(id);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
