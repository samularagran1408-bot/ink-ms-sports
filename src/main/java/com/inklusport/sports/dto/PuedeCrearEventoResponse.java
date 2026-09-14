package com.inklusport.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuedeCrearEventoResponse {
    private boolean puedeCrear;
    private Integer eventosCreadosMes;
    private Integer limiteEventosMes;
    private String planNombre;
}
