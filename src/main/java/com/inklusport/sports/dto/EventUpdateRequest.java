package com.inklusport.sports.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Campos opcionales para actualizar un evento.
 * Si cambian fecha, hora o ubicación se notifica a los inscritos.
 */
@Data
public class EventUpdateRequest {

    private Integer sportId;
    private String name;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private String imageUrl;
    private Double latitude;
    private Double longitude;

    @Positive(message = "El cupo máximo debe ser mayor a 0")
    private Integer maxCapacity;
}
