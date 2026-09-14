package com.inklusport.sports.controller;

import com.inklusport.sports.dto.RoutineAttendanceRequest;
import com.inklusport.sports.dto.RoutineRegistrationRequest;
import com.inklusport.sports.dto.RoutineRegistrationResponse;
import com.inklusport.sports.service.RoutineRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routine-registrations")
@RequiredArgsConstructor
public class RoutineRegistrationController {

    private final RoutineRegistrationService registrationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> register(@Valid @RequestBody RoutineRegistrationRequest request) {
        try {
            RoutineRegistrationResponse response = registrationService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        try {
            registrationService.cancel(id);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Inscripción a la rutina cancelada."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RoutineRegistrationResponse>> byUser(@PathVariable String userId) {
        return ResponseEntity.ok(registrationService.byUser(userId));
    }

    @PostMapping("/{id}/attendance")
    @PreAuthorize("hasRole('COACH') or hasRole('ADMIN') or hasRole('ENTRENADOR')")
    public ResponseEntity<?> markAttendance(
            @PathVariable String id,
            @RequestBody(required = false) RoutineAttendanceRequest request
    ) {
        try {
            boolean attended = request == null || request.getAttended() == null || Boolean.TRUE.equals(request.getAttended());
            return ResponseEntity.ok(registrationService.markAttendance(id, attended));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }
}
