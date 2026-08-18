package com.inklusport.sports.service;

import com.inklusport.sports.dto.DisabilityRequest;
import com.inklusport.sports.entity.Disability;
import com.inklusport.sports.exception.ResourceNotFoundException;
import com.inklusport.sports.repository.DisabilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisabilityServiceTest {

    @Mock
    private DisabilityRepository disabilityRepository;

    @InjectMocks
    private DisabilityService disabilityService;

    @Test
    void createDisabilityRejectsDuplicateName() {
        DisabilityRequest request = new DisabilityRequest();
        request.setName("Visual");
        when(disabilityRepository.existsByNameIgnoreCase("Visual")).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> disabilityService.createDisability(request)
        );
        assertTrue(error.getMessage().contains("Ya existe"));
        verify(disabilityRepository, never()).save(any());
    }

    @Test
    void getDisabilityByIdThrowsWhenMissing() {
        when(disabilityRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> disabilityService.getDisabilityById(99)
        );
        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    void updateDisabilityRejectsDuplicateName() {
        Disability current = Disability.builder().id(1).name("Visual").isActive(true).build();
        DisabilityRequest request = new DisabilityRequest();
        request.setName("Auditiva");
        when(disabilityRepository.findById(1)).thenReturn(Optional.of(current));
        when(disabilityRepository.existsByNameIgnoreCaseAndIdNot("Auditiva", 1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> disabilityService.updateDisability(1, request));
    }

    @Test
    void deactivateAndReactivateDisability() {
        Disability current = Disability.builder().id(1).name("Visual").isActive(true).build();
        when(disabilityRepository.findById(1)).thenReturn(Optional.of(current));
        when(disabilityRepository.save(any(Disability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(disabilityService.deactivateDisability(1).getIsActive());
        assertTrue(disabilityService.activateDisability(1).getIsActive());
    }

    @Test
    void deactivateAlreadyInactiveFails() {
        Disability current = Disability.builder().id(1).name("Visual").isActive(false).build();
        when(disabilityRepository.findById(1)).thenReturn(Optional.of(current));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> disabilityService.deactivateDisability(1)
        );
        assertEquals("La discapacidad ya está desactivada.", error.getMessage());
    }
}
