package com.inklusport.sports.service;

import com.inklusport.sports.dto.DisabilityRequest;
import com.inklusport.sports.dto.DisabilityResponse;
import com.inklusport.sports.entity.Disability;
import com.inklusport.sports.repository.DisabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisabilityService {

    private final DisabilityRepository disabilityRepository;

    @Transactional(readOnly = true)
    public List<DisabilityResponse> getAllDisabilities() {
        return disabilityRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DisabilityResponse> getActiveDisabilities() {
        return disabilityRepository.findByIsActiveTrue().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DisabilityResponse getDisabilityById(Integer id) {
        Disability d = disabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discapacidad no encontrada con ID: " + id));
        return convertToResponse(d);
    }

    @Transactional
    public DisabilityResponse createDisability(DisabilityRequest request) {
        if (disabilityRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ya existe esta discapacidad");
        }
        Disability d = Disability.builder()
                .name(request.getName()).description(request.getDescription())
                .category(request.getCategory()).isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public DisabilityResponse updateDisability(Integer id, DisabilityRequest request) {
        Disability d = disabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discapacidad no encontrada"));
        d.setName(request.getName());
        d.setDescription(request.getDescription());
        d.setCategory(request.getCategory());
        d.setIsActive(request.getIsActive());
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public void deleteDisability(Integer id) {
        if (!disabilityRepository.existsById(id)) throw new RuntimeException("No existe");
        disabilityRepository.deleteById(id);
    }

    private DisabilityResponse convertToResponse(Disability d) {
        return DisabilityResponse.builder().id(d.getId()).name(d.getName())
                .description(d.getDescription()).category(d.getCategory()).isActive(d.getIsActive()).build();
    }
}