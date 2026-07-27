package com.inklusport.sports.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaitlistRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private String userId;

    @NotNull(message = "El ID del evento es obligatorio")
    private String eventId;
}