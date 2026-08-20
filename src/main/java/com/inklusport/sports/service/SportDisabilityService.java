package com.inklusport.sports.service;

import com.inklusport.sports.dto.SportDisabilityRequest;
import com.inklusport.sports.dto.SportDisabilityResponse;
import com.inklusport.sports.entity.Disability;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.entity.SportDisability;
import com.inklusport.sports.exception.ResourceNotFoundException;
import com.inklusport.sports.repository.DisabilityRepository;
import com.inklusport.sports.repository.SportDisabilityRepository;
import com.inklusport.sports.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SportDisabilityService {

    private final SportDisabilityRepository sportDisabilityRepository;
    private final SportRepository sportRepository;
    private final DisabilityRepository disabilityRepository;

    @Transactional(readOnly = true)
    public List<SportDisabilityResponse> getSportDisabilities(Integer sportId) {
        if (!sportRepository.existsById(sportId)) {
            throw new ResourceNotFoundException("Deporte no encontrado con ID: " + sportId);
        }
        return sportDisabilityRepository.findBySportId(sportId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SportDisabilityResponse> getAllAssociations() {
        return sportDisabilityRepository.findAllWithActiveDisabilities().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SportDisabilityResponse> searchAssociations(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return getAllAssociations();
        }
        return sportDisabilityRepository.searchActive(q).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SportDisabilityResponse addAdaptation(SportDisabilityRequest request) {
        /**
         * conversión a Integer usando .intValue() por si el Request expone un Long
         */
        Integer sId = request.getSportId() instanceof Long ? ((Long) (Object) request.getSportId()).intValue() : (Integer) (Object) request.getSportId();
        Integer dId = request.getDisabilityId() instanceof Long ? ((Long) (Object) request.getDisabilityId()).intValue() : (Integer) (Object) request.getDisabilityId();

        Sport sport = sportRepository.findById(sId)
                .orElseThrow(() -> new ResourceNotFoundException("Deporte no encontrado"));
        Disability dis = disabilityRepository.findById(dId)
                .orElseThrow(() -> new ResourceNotFoundException("Discapacidad no encontrada"));
        if (Boolean.FALSE.equals(dis.getIsActive())) {
            throw new IllegalStateException("No se puede asociar una discapacidad desactivada.");
        }
        
        SportDisability.SportDisabilityId id = new SportDisability.SportDisabilityId(sId, dId);
        SportDisability sd = SportDisability.builder()
                .id(id)
                .sport(sport)
                .disability(dis)
                .adaptations(request.getAdaptations())
                .build();
                
        return convertToResponse(sportDisabilityRepository.save(sd));
    }

    @Transactional
    public SportDisabilityResponse updateAdaptation(Integer sportId, Integer disabilityId, SportDisabilityRequest request) {
        SportDisability.SportDisabilityId id = new SportDisability.SportDisabilityId(sportId, disabilityId);
        SportDisability sd = sportDisabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relación no encontrada"));
        sd.setAdaptations(request.getAdaptations());
        return convertToResponse(sportDisabilityRepository.save(sd));
    }

    @Transactional
    public void removeAdaptation(Integer sportId, Integer disabilityId) {
        SportDisability.SportDisabilityId id = new SportDisability.SportDisabilityId(sportId, disabilityId);
        sportDisabilityRepository.deleteById(id);
    }

    private SportDisabilityResponse convertToResponse(SportDisability sd) {
        /**
         * valores numéricos y los casteamos de forma segura
         */
        Integer sId = sd.getSport().getId();
        Integer dId = sd.getDisability().getId();

        return SportDisabilityResponse.builder()
                .sportId(sId)
                .sportName(sd.getSport().getName())
                .disabilityId(dId)
                .disabilityName(sd.getDisability().getName())
                .adaptations(sd.getAdaptations())
                .build();
    }
}