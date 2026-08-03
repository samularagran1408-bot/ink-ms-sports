package com.inklusport.sports.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutineRegistrationRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private String userId;

    @NotNull(message = "El ID de la rutina es obligatorio")
    private String routineId;
}
