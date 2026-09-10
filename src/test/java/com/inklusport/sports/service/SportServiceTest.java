package com.inklusport.sports.service;

import com.inklusport.sports.dto.SportRequest;
import com.inklusport.sports.dto.SportResponse;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.repository.SportDisabilityRepository;
import com.inklusport.sports.repository.SportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportServiceTest {

    @Mock
    private SportRepository sportRepository;
    @Mock
    private SportDisabilityRepository sportDisabilityRepository;

    @InjectMocks
    private SportService sportService;

    @Test
    void createSport_rechazaNombreDuplicado() {
        SportRequest request = new SportRequest();
        request.setName("Natación");
        when(sportRepository.existsByName("Natación")).thenReturn(true);

        RuntimeException error = assertThrows(RuntimeException.class, () -> sportService.createSport(request));
        assertTrue(error.getMessage().contains("Ya existe"));
        verify(sportRepository, never()).save(any());
    }

    @Test
    void createSport_guardaConDificultadAlta() {
        SportRequest request = new SportRequest();
        request.setName("Natación");
        request.setDescription("Piscina");
        request.setDifficulty("avanzado");

        when(sportRepository.existsByName("Natación")).thenReturn(false);
        when(sportRepository.save(any(Sport.class))).thenAnswer(invocation -> {
            Sport sport = invocation.getArgument(0);
            sport.setId(7);
            return sport;
        });
        when(sportDisabilityRepository.findDisabilitiesBySportId(7)).thenReturn(List.of());

        SportResponse response = sportService.createSport(request);

        assertEquals(7, response.getId());
        assertEquals("Natación", response.getName());
        assertEquals("alto", response.getDifficulty());
        assertTrue(response.getIsActive());
    }

    @Test
    void getSportById_lanzaSiNoExiste() {
        when(sportRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () -> sportService.getSportById(99));
        assertTrue(error.getMessage().contains("99"));
    }
}
