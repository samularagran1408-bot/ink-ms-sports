package com.inklusport.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisabilityRequest {

    @NotBlank(message = "El nombre de la discapacidad es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    private String description;

    private String category; /** visual, fisica, auditiva, intelectual, multiple */ 

    private Boolean isActive;
}