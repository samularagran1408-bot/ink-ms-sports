package com.inklusport.sports.service;

import com.inklusport.sports.dto.SportRequest;
import com.inklusport.sports.dto.DisabilityResponse;
import com.inklusport.sports.dto.SportResponse;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.repository.SportDisabilityRepository;
import com.inklusport.sports.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportService {

    private final SportRepository sportRepository;
    private final SportDisabilityRepository sportDisabilityRepository;

    @Transactional(readOnly = true)
    public List<SportResponse> getAllSports() {
        return sportRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SportResponse> getActiveSports() {
        return sportRepository.findByIsActiveTrue().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SportResponse getSportById(Integer id) {
        Sport sport = sportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deporte no encontrado con ID: " + id));
        return convertToResponse(sport);
    }

    @Transactional
    public SportResponse createSport(SportRequest request) {
        if (sportRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe un deporte con el nombre: " + request.getName());
        }
        Sport sport = Sport.builder()
                .name(request.getName())
                .description(request.getDescription())
                .requiredMaterials(request.getRequiredMaterials())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        if (request.getDifficulty() != null) {
            sport.setDifficulty(Sport.DifficultyLevel.valueOf(request.getDifficulty()));
        }
        return convertToResponse(sportRepository.save(sport));
    }

    @Transactional
    public SportResponse updateSport(Integer id, SportRequest request) {
        Sport sport = sportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deporte no encontrado con ID: " + id));
        sport.setName(request.getName());
        sport.setDescription(request.getDescription());
        if (request.getDifficulty() != null) {
            sport.setDifficulty(Sport.DifficultyLevel.valueOf(request.getDifficulty()));
        }
        sport.setRequiredMaterials(request.getRequiredMaterials());
        sport.setIsActive(request.getIsActive());
        return convertToResponse(sportRepository.save(sport));
    }

    @Transactional
    public void deleteSport(Integer id) {
        if (!sportRepository.existsById(id)) {
            throw new RuntimeException("Deporte no encontrado con ID: " + id);
        }
        sportRepository.deleteById(id);
    }

    private SportResponse convertToResponse(Sport sport) {
        List<DisabilityResponse> disabilities = sportDisabilityRepository.findDisabilitiesBySportId(sport.getId())
                .stream().map(d -> DisabilityResponse.builder()
                        .id(d.getId()).name(d.getName()).description(d.getDescription())
                        .category(d.getCategory()).isActive(d.getIsActive()).build())
                .collect(Collectors.toList());

        return SportResponse.builder()
                .id(sport.getId()).name(sport.getName()).description(sport.getDescription())
                .difficulty(sport.getDifficulty() != null ? sport.getDifficulty().name() : null)
                .requiredMaterials(sport.getRequiredMaterials()).isActive(sport.getIsActive())
                .createdAt(sport.getCreatedAt()).disabilities(disabilities).build();
    }
}