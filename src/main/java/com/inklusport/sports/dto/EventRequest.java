package com.inklusport.sports.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventRequest {

    @NotNull(message = "El ID del deporte es obligatorio")
    private Integer sportId;

    @NotBlank(message = "El nombre del evento es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "La fecha del evento es obligatoria")
    @Future(message = "La fecha debe ser futura")
    private LocalDate eventDate;

    @NotNull(message = "La hora del evento es obligatoria")
    private LocalTime eventTime;

    private String location;

    /** Opcional; si falta se asigna una foto predeterminada según el deporte. */
    private String imageUrl;

    /** Coordenadas de Google Maps (Places). */
    private Double latitude;
    private Double longitude;

    @NotNull(message = "El cupo máximo es obligatorio")
    @Positive(message = "El cupo máximo debe ser mayor a 0")
    private Integer maxCapacity;

    private String createdBy;
}