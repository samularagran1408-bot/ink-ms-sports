package com.inklusport.sports.controller;

import com.inklusport.sports.dto.FutureRegistrationsCheckResponse;
import com.inklusport.sports.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas internas entre microservicios (users → sports).
 */
@RestController
@RequestMapping("/api/internal/registrations")
@RequiredArgsConstructor
public class InternalRegistrationController {

    private final RegistrationService registrationService;

    @GetMapping("/user/{userId}/future")
    public FutureRegistrationsCheckResponse futureRegistrations(@PathVariable String userId) {
        return registrationService.checkFutureRegistrations(userId);
    }
}
