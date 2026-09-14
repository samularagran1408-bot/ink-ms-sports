package com.inklusport.sports.client;

import com.inklusport.sports.dto.PuedeCrearEventoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(
        name = "ink-ms-subscriptions",
        url = "${subscriptions.service.url:http://localhost:3005}")
public interface SubscriptionsServiceClient {

    @GetMapping("/api/internal/suscripciones/organizadores/{organizadorId}/puede-crear-evento")
    PuedeCrearEventoResponse puedeCrearEvento(@PathVariable("organizadorId") String organizadorId);

    @PostMapping("/api/internal/suscripciones/organizadores/{organizadorId}/registrar-evento-creado")
    void registrarEventoCreado(@PathVariable("organizadorId") String organizadorId);

    @GetMapping("/api/internal/suscripciones/eventos/{eventoId}/pago")
    Map<String, Object> configuracionPago(@PathVariable("eventoId") String eventoId);
}
