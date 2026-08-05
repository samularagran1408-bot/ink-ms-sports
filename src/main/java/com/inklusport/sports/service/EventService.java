package com.inklusport.sports.service;

import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.dto.EventUpdateRequest;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int HORAS_DESPUES = 2;
    private static final int HORAS_RETENCION = 24;
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final EventRepository eventRepository;
    private final SportRepository sportRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final StaffNotificationService staffNotificationService;
    private final QuizEligibilityService quizEligibilityService;

    @Transactional
    public List<EventResponse> getAllEvents() {
        procesarEstadosEventos();
        return eventRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * Crea un evento en borrador; exige quiz de organizador aprobado (salvo admin).
     */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        quizEligibilityService.assertOrganizerQuizPassed(request.getCreatedBy());
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
     * Actualiza un evento. Exige quiz de organizador.
     * Si cambian fecha, hora o lugar, notifica a inscritos y admins.
     */
    @Transactional
    public EventResponse updateEvent(String eventId, EventUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        quizEligibilityService.assertOrganizerQuizPassed(event.getCreatedBy());

        LocalDate oldDate = event.getEventDate();
        LocalTime oldTime = event.getEventTime();
        String oldLocation = event.getLocation();

        if (request.getSportId() != null) {
            sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new RuntimeException("Deporte no encontrado"));
            event.setSportId(request.getSportId());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            event.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getEventTime() != null) {
            event.setEventTime(request.getEventTime());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation().isBlank() ? null : request.getLocation().trim());
        }
        if (request.getMaxCapacity() != null) {
            int occupied = event.getMaxCapacity() - (event.getAvailableCapacity() != null
                    ? event.getAvailableCapacity() : event.getMaxCapacity());
            if (request.getMaxCapacity() < occupied) {
                throw new IllegalStateException(
                        "El cupo máximo no puede ser menor a las inscripciones confirmadas (" + occupied + ").");
            }
            event.setMaxCapacity(request.getMaxCapacity());
            event.setAvailableCapacity(request.getMaxCapacity() - occupied);
        }

        Event saved = eventRepository.saveAndFlush(event);

        boolean dateChanged = !Objects.equals(oldDate, saved.getEventDate())
                || !Objects.equals(oldTime, saved.getEventTime());
        boolean locationChanged = !Objects.equals(
                normalizeLocation(oldLocation),
                normalizeLocation(saved.getLocation()));

        if (dateChanged || locationChanged) {
            notifyRegistrantsAboutScheduleOrLocationChange(
                    saved, oldDate, oldTime, oldLocation, dateChanged, locationChanged);
        }

        procesarEstadosEventos();
        return convertToResponse(eventRepository.findById(saved.getId()).orElse(saved));
    }

    private void notifyRegistrantsAboutScheduleOrLocationChange(
            Event event,
            LocalDate oldDate,
            LocalTime oldTime,
            String oldLocation,
            boolean dateChanged,
            boolean locationChanged) {

        List<String> changes = new ArrayList<>();
        if (dateChanged) {
            changes.add("fecha/hora: de " + formatDateTime(oldDate, oldTime)
                    + " a " + formatDateTime(event.getEventDate(), event.getEventTime()));
        }
        if (locationChanged) {
            changes.add("lugar: de \"" + displayLocation(oldLocation)
                    + "\" a \"" + displayLocation(event.getLocation()) + "\"");
        }

        String changeSummary = String.join("; ", changes);
        String title = dateChanged && locationChanged
                ? "Cambio de fecha y lugar del evento"
                : (dateChanged ? "Cambio de fecha del evento" : "Cambio de lugar del evento");
        String body = "El evento \"" + event.getName() + "\" se actualizó (" + changeSummary + ").";

        List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(event.getId());
        for (EventRegistration reg : registrations) {
            staffNotificationService.notifyUser(
                    reg.getUserId(),
                    "event_updated",
                    title,
                    body,
                    event.getId()
            );
        }

        staffNotificationService.notifyAdmins(
                "admin_event_updated",
                title,
                body + " Inscritos notificados: " + registrations.size() + ".",
                event.getId()
        );

        log.info("Notificados {} inscritos por cambio en evento {}", registrations.size(), event.getId());
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        if (date == null) {
            return "sin fecha";
        }
        String d = date.format(FECHA_FMT);
        if (time == null) {
            return d;
        }
        return d + " " + time.format(HORA_FMT);
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        return location.trim();
    }

    private String displayLocation(String location) {
        String normalized = normalizeLocation(location);
        return normalized.isEmpty() ? "sin ubicación" : normalized;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    private LocalDateTime fechaHoraEvento(Event event) {
        return event.getEventDate().atTime(event.getEventTime());
    }

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
