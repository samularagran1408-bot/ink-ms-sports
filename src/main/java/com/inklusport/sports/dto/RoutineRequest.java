package com.inklusport.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RoutineRequest {

    private String trainerId;

    private Integer sportId;

    @NotBlank(message = "El nombre de la rutina es obligatorio")
    private String name;

    private String description;

    private String disabilityFocus;

    private String level;

    @Positive(message = "La duración debe ser mayor a 0")
    private Integer durationMinutes;

    /** JSON serializado de ejercicios (array u objeto). */
    private String exercisesJson;

    @Positive(message = "El cupo máximo debe ser mayor a 0")
    private Integer maxCapacity;
}
