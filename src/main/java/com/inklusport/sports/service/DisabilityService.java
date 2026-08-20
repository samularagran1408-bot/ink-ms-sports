package com.inklusport.sports.service;

import com.inklusport.sports.dto.DisabilityRequest;
import com.inklusport.sports.dto.DisabilityResponse;
import com.inklusport.sports.entity.Disability;
import com.inklusport.sports.exception.ResourceNotFoundException;
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
        return convertToResponse(requireDisability(id));
    }

    /**
     * Búsqueda pública: solo discapacidades activas. Por ID numérico o por nombre.
     * Las desactivadas se tratan como no encontradas.
     */
    @Transactional(readOnly = true)
    public List<DisabilityResponse> searchActiveDisabilities(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return getActiveDisabilities();
        }
        if (q.chars().allMatch(Character::isDigit)) {
            Integer id = Integer.valueOf(q);
            Disability disability = disabilityRepository.findByIdAndIsActiveTrue(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            disabilityRepository.existsById(id)
                                    ? "No se puede buscar una discapacidad desactivada."
                                    : "Discapacidad no encontrada con ID: " + id));
            return List.of(convertToResponse(disability));
        }
        return disabilityRepository.findByIsActiveTrueAndNameContainingIgnoreCase(q).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DisabilityResponse createDisability(DisabilityRequest request) {
        String name = normalizeName(request.getName());
        if (disabilityRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalStateException("Ya existe una discapacidad con el nombre: " + name);
        }
        Disability d = Disability.builder()
                .name(name)
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public DisabilityResponse updateDisability(Integer id, DisabilityRequest request) {
        Disability d = requireDisability(id);
        String name = normalizeName(request.getName());
        if (disabilityRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalStateException("Ya existe una discapacidad con el nombre: " + name);
        }
        d.setName(name);
        d.setDescription(request.getDescription());
        d.setCategory(request.getCategory());
        if (request.getIsActive() != null) {
            d.setIsActive(request.getIsActive());
        }
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public DisabilityResponse deactivateDisability(Integer id) {
        Disability d = requireDisability(id);
        if (Boolean.FALSE.equals(d.getIsActive())) {
            throw new IllegalStateException("La discapacidad ya está desactivada.");
        }
        d.setIsActive(false);
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public DisabilityResponse activateDisability(Integer id) {
        Disability d = requireDisability(id);
        if (Boolean.TRUE.equals(d.getIsActive())) {
            throw new IllegalStateException("La discapacidad ya está activa.");
        }
        d.setIsActive(true);
        return convertToResponse(disabilityRepository.save(d));
    }

    @Transactional
    public void deleteDisability(Integer id) {
        if (!disabilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Discapacidad no encontrada con ID: " + id);
        }
        disabilityRepository.deleteById(id);
    }

    private Disability requireDisability(Integer id) {
        return disabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discapacidad no encontrada con ID: " + id));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la discapacidad es obligatorio.");
        }
        return name.trim();
    }

    private DisabilityResponse convertToResponse(Disability d) {
        return DisabilityResponse.builder().id(d.getId()).name(d.getName())
                .description(d.getDescription()).category(d.getCategory()).isActive(d.getIsActive()).build();
    }
}
