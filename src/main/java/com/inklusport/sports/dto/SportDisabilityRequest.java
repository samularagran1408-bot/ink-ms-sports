package com.inklusport.sports.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SportDisabilityRequest {

    @NotNull(message = "El ID del deporte es obligatorio")
    private Long sportId;

    @NotNull(message = "El ID de la discapacidad es obligatorio")
    private Long disabilityId;

    @NotBlank(message = "Las adaptaciones son obligatorias")
    private String adaptations;
}