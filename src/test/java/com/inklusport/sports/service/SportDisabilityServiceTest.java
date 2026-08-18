package com.inklusport.sports.service;

import com.inklusport.sports.exception.ResourceNotFoundException;
import com.inklusport.sports.repository.DisabilityRepository;
import com.inklusport.sports.repository.SportDisabilityRepository;
import com.inklusport.sports.repository.SportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportDisabilityServiceTest {

    @Mock
    private SportDisabilityRepository sportDisabilityRepository;
    @Mock
    private SportRepository sportRepository;
    @Mock
    private DisabilityRepository disabilityRepository;

    @InjectMocks
    private SportDisabilityService sportDisabilityService;

    @Test
    void getSportDisabilitiesReturnsEmptyWhenSportHasNone() {
        when(sportRepository.existsById(1)).thenReturn(true);
        when(sportDisabilityRepository.findBySportId(1)).thenReturn(Collections.emptyList());

        assertTrue(sportDisabilityService.getSportDisabilities(1).isEmpty());
    }

    @Test
    void getSportDisabilitiesThrowsWhenSportMissing() {
        when(sportRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> sportDisabilityService.getSportDisabilities(99));
    }
}
