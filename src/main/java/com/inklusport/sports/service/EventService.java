package com.inklusport.sports.service;

import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    /**
     * Zona horaria de Bogotá
     */
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /**
     * Horas después de finalizar para finalizar el evento
     */
    private static final int HORAS_DESPUES = 2;
    /**
     * Horas de retención para eliminar el evento
     */
    private static final int HORAS_RETENCION = 24;

    /**
     * Repositorio de eventos
     */
    private final EventRepository eventRepository;
    /**
     * Repositorio de deportes
     */
    private final SportRepository sportRepository;
    /**
     * Repositorio de asistencias
     */
    private final EventAttendanceRepository eventAttendanceRepository;

    /**
     * Obtiene todos los eventos
     * @return Lista de eventos
     */
    @Transactional
    public List<EventResponse> getAllEvents() {
        procesarEstadosEventos();
        return eventRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * Crea un evento
     * @param request Request con los datos del evento
     * @return Evento creado
     */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new RuntimeException("Deporte no encontrado"));
        Event event = Event.builder()
                .sportId(sport.getId())
                .name(request.getName())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .eventTime(request.getEventTime())
                .location(request.getLocation())
                .maxCapacity(request.getMaxCapacity())
                .availableCapacity(request.getMaxCapacity())
                .createdBy(request.getCreatedBy())
                .status(EventStatus.draft)
                .build();
        Event saved = eventRepository.save(event);
        procesarEstadosEventos();
        return convertToResponse(eventRepository.findById(saved.getId()).orElse(saved));
    }

    /**
     * Obtiene la fecha y hora actual
     * @return Fecha y hora actual
     */
    private LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    /**
     * Obtiene la fecha y hora del evento
     * @param event Evento
     * @return Fecha y hora del evento
     */
    private LocalDateTime fechaHoraEvento(Event event) {
        return event.getEventDate().atTime(event.getEventTime());
    }

    /**
     * Activa los eventos
     * @return Cantidad de eventos activados
     */
    @Transactional
    public int activarEventos() {
        log.info("[EventService] Activando eventos (DRAFT → ACTIVE)");

        LocalDateTime ahora = ahora();
        log.info("Buscando eventos DRAFT para activar - Referencia: {}", ahora);

        List<Event> eventosAActivar = eventRepository.findByStatus(EventStatus.draft).stream()
                .filter(evento -> !fechaHoraEvento(evento).isAfter(ahora))
                .toList();

        if (eventosAActivar.isEmpty()) {
            log.info("[EventService] No hay eventos para activar");
            return 0;
        }

        log.info("[EventService] Eventos encontrados para activar: {}", eventosAActivar.size());

        int contador = 0;
        for (Event evento : eventosAActivar) {
            evento.setStatus(EventStatus.active);
            eventRepository.save(evento);
            contador++;
            log.info("[EventService] Evento activado: '{}' (Fecha: {}, Hora: {})",
                evento.getName(), evento.getEventDate(), evento.getEventTime());
        }

        log.info("[EventService] Total eventos activados: {}", contador);
        return contador;
    }

    /**
     * Finaliza los eventos
     * @return Cantidad de eventos finalizados
     */
    @Transactional
    public int finalizarEventos() {
        log.info("[EventService] Finalizando eventos (ACTIVE → FINISHED)");

        LocalDateTime ahora = ahora();
        log.info("Buscando eventos ACTIVE para finalizar - Referencia: {}", ahora);

        List<Event> eventosAFinalizar = eventRepository.findByStatus(EventStatus.active).stream()
                .filter(evento -> !fechaHoraEvento(evento).plusHours(HORAS_DESPUES).isAfter(ahora))
                .toList();

        if (eventosAFinalizar.isEmpty()) {
            log.info("[EventService] No hay eventos para finalizar");
            return 0;
        }

        log.info("[EventService] Eventos encontrados para finalizar: {}", eventosAFinalizar.size());

        int contador = 0;
        for (Event evento : eventosAFinalizar) {
            evento.setStatus(EventStatus.finished);
            eventRepository.save(evento);
            contador++;
            log.info("[EventService] Evento finalizado: '{}' (Fecha: {}, Hora: {})",
                evento.getName(), evento.getEventDate(), evento.getEventTime());
        }

        log.info("[EventService] Total eventos finalizados: {}", contador);
        return contador;
    }

    /**
     * Elimina los eventos expirados
     * @return Cantidad de eventos eliminados
     */
    @Transactional
    public int eliminarEventosExpirados() {
        log.info("[EventService] Eliminando eventos finalizados (>24h después de finalizar)");

        LocalDateTime ahora = ahora();
        List<Event> eventosAEliminar = eventRepository.findByStatus(EventStatus.finished).stream()
                .filter(evento -> !fechaHoraEvento(evento)
                        .plusHours(HORAS_DESPUES)
                        .plusHours(HORAS_RETENCION)
                        .isAfter(ahora))
                .toList();

        if (eventosAEliminar.isEmpty()) {
            log.info("[EventService] No hay eventos para eliminar");
            return 0;
        }

        log.info("[EventService] Eventos encontrados para eliminar: {}", eventosAEliminar.size());

        int contador = 0;
        for (Event evento : eventosAEliminar) {
            eventAttendanceRepository.deleteAll(
                    eventAttendanceRepository.findByRegistration_EventId(evento.getId()));
            eventRepository.delete(evento);
            contador++;
            log.info("[EventService] Evento eliminado: '{}' (Fecha: {}, Hora: {})",
                    evento.getName(), evento.getEventDate(), evento.getEventTime());
        }

        log.info("[EventService] Total eventos eliminados: {}", contador);
        return contador;
    }

    
    /**
     * Procesa los estados de eventos
     */
    @Transactional
    public void procesarEstadosEventos() {
        log.info("[EventService] Procesando estados de eventos");
        
        int activados = activarEventos();
        int finalizados = finalizarEventos();
        int eliminados = eliminarEventosExpirados();

        if (activados > 0 || finalizados > 0 || eliminados > 0) {
            log.info("[EventService] Resumen: Activados: {}, Finalizados: {}, Eliminados: {}",
                    activados, finalizados, eliminados);
        }
    }

    /**
     * Convierte un evento a un response
     * @param event Evento
     * @return Response con los datos del evento
     */
    private EventResponse convertToResponse(Event event) {
        Sport sport = sportRepository.findById(event.getSportId()).orElse(null);
        return EventResponse.builder()
                .id(event.getId())
                .sportId(event.getSportId())
                .sportName(sport != null ? sport.getName() : "N/A")
                .name(event.getName())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventTime(event.getEventTime())
                .location(event.getLocation())
                .maxCapacity(event.getMaxCapacity())
                .availableCapacity(event.getAvailableCapacity())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .build();
    }
}