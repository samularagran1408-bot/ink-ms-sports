package com.inklusport.sports.service;

import com.inklusport.sports.dto.CalendarEventResponse;
import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventResponse;
import com.inklusport.sports.dto.EventUpdateRequest;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.exception.ResourceNotFoundException;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.repository.SportRepository;
import com.inklusport.sports.util.EventImageDefaults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

/** Ciclo de vida de eventos: catálogo, calendario y transiciones de estado. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int HORAS_DESPUES = 2;
    private static final int HORAS_RETENCION = 24;
    static final int MAX_EVENT_CAPACITY = 500;
    static final List<EventStatus> CATALOG_STATUSES = List.of(EventStatus.draft, EventStatus.active);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final EventRepository eventRepository;
    private final SportRepository sportRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final StaffNotificationService staffNotificationService;
    private final QuizEligibilityService quizEligibilityService;

    /** Lista todos los eventos tras actualizar estados (activa/finaliza/elimina). */
    @Transactional
    public List<EventResponse> getAllEvents() {
        procesarEstadosEventos();
        return eventRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /** Obtiene un evento por ID; lanza si no existe. */
    @Transactional(readOnly = true)
    public EventResponse getEventById(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        return convertToResponse(event);
    }

    /**
     * Eventos vigentes para consultar/inscribirse: draft (recién creados) y active.
     * Pasan a active el día y hora del evento; finished se elimina 24 h después.
     * Cancelled permanece 2 h y luego se elimina.
     */
    @Transactional
    public List<EventResponse> getAvailableEvents() {
        procesarEstadosEventos();
        LocalDate today = LocalDate.now(ZONA);
        return eventRepository.findByStatusInOrderByEventDateAscEventTimeAsc(CATALOG_STATUSES).stream()
                .filter(event -> event.getEventDate() != null && !event.getEventDate().isBefore(today))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calendario de eventos visibles (draft y active), opcionalmente filtrado por rango.
     */
    @Transactional
    public List<CalendarEventResponse> getCalendar(LocalDate fromDate, LocalDate toDate) {
        procesarEstadosEventos();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return eventRepository.findCalendarEvents(CATALOG_STATUSES, fromDate, toDate).stream()
                .map(this::convertToCalendarResponse)
                .collect(Collectors.toList());
    }

    /**
     * Búsqueda de eventos draft/active por texto y rango de fechas. Sin coincidencias → lista vacía.
     */
    @Transactional
    public List<EventResponse> searchEvents(String query, LocalDate fromDate, LocalDate toDate) {
        procesarEstadosEventos();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        String q = query == null ? "" : query.trim();
        return eventRepository.searchEvents(q, CATALOG_STATUSES, fromDate, toDate).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancela un evento activo o en borrador. Falla si ya está cancelado o finalizado.
     */
    @Transactional
    public EventResponse cancelEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        quizEligibilityService.assertOrganizerQuizPassed(event.getCreatedBy());

        if (event.getStatus() == EventStatus.cancelled) {
            throw new IllegalStateException("El evento ya está cancelado.");
        }
        if (event.getStatus() == EventStatus.finished) {
            throw new IllegalStateException("No se puede cancelar un evento finalizado.");
        }

        event.setStatus(EventStatus.cancelled);
        event.setCancelledAt(ahora());
        Event saved = eventRepository.saveAndFlush(event);
        notifyRegistrantsAboutCancellation(saved);
        return convertToResponse(saved);
    }

    /**
     * Crea un evento en borrador; exige quiz de organizador aprobado (salvo admin).
     */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        quizEligibilityService.assertOrganizerQuizPassed(request.getCreatedBy());
        validateEventDateTime(request.getEventDate(), request.getEventTime());
        validateCapacity(request.getMaxCapacity());
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Deporte no encontrado"));
        String imageUrl = request.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = EventImageDefaults.forSport(sport.getName());
        }
        Event event = Event.builder()
                .sportId(sport.getId())
                .name(request.getName())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .eventTime(request.getEventTime())
                .location(request.getLocation())
                .imageUrl(imageUrl.trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
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
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        quizEligibilityService.assertOrganizerQuizPassed(event.getCreatedBy());

        LocalDate oldDate = event.getEventDate();
        LocalTime oldTime = event.getEventTime();
        String oldLocation = event.getLocation();

        if (request.getSportId() != null) {
            Sport newSport = sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deporte no encontrado"));
            event.setSportId(request.getSportId());
            // Si la portada es la predeterminada, actualízala al cambiar de deporte.
            if (event.getImageUrl() == null || event.getImageUrl().isBlank()
                    || event.getImageUrl().startsWith("assets/events/")) {
                event.setImageUrl(EventImageDefaults.forSport(newSport.getName()));
            }
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
        if (request.getLatitude() != null) {
            event.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            event.setLongitude(request.getLongitude());
        }
        // Si borran la dirección, limpian coordenadas.
        if (request.getLocation() != null && request.getLocation().isBlank()) {
            event.setLatitude(null);
            event.setLongitude(null);
        }
        if (request.getImageUrl() != null) {
            event.setImageUrl(request.getImageUrl().isBlank() ? null : request.getImageUrl().trim());
        }
        if (request.getMaxCapacity() != null) {
            validateCapacity(request.getMaxCapacity());
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

    /** Notifica inscritos y admins un cambio de fecha, hora o lugar. */
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

    /** Notifica inscritos y admins la cancelación del evento. */
    private void notifyRegistrantsAboutCancellation(Event event) {
        String title = "Evento cancelado";
        String body = "El evento \"" + event.getName() + "\" fue cancelado.";
        List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(event.getId());
        for (EventRegistration reg : registrations) {
            staffNotificationService.notifyUser(
                    reg.getUserId(),
                    "event_cancelled",
                    title,
                    body,
                    event.getId()
            );
        }
        staffNotificationService.notifyAdmins(
                "admin_event_cancelled",
                title,
                body + " Inscritos notificados: " + registrations.size() + ".",
                event.getId()
        );
    }

    /** Valida el cupo máximo permitido; lanza si es inválido o excede el tope. */
    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity == null || maxCapacity <= 0) {
            throw new IllegalArgumentException("El cupo máximo debe ser mayor a 0.");
        }
        if (maxCapacity > MAX_EVENT_CAPACITY) {
            throw new IllegalStateException(
                    "El cupo del evento está excedido. El máximo permitido es " + MAX_EVENT_CAPACITY + ".");
        }
    }

    /**
     * Permite crear un evento para hoy si la hora todavía no ha pasado.
     */
    private void validateEventDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new IllegalArgumentException("La fecha y la hora del evento son obligatorias.");
        }
        if (!date.atTime(time).isAfter(ahora())) {
            throw new IllegalArgumentException("La fecha y hora del evento deben ser futuras.");
        }
    }

    /** Formatea fecha y hora para mensajes de notificación. */
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

    /** Normaliza el lugar (trim) o cadena vacía si no hay valor. */
    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        return location.trim();
    }

    /** Texto amigable del lugar, o "sin ubicación" si está vacío. */
    private String displayLocation(String location) {
        String normalized = normalizeLocation(location);
        return normalized.isEmpty() ? "sin ubicación" : normalized;
    }

    /** Fecha-hora actual en zona America/Bogota. */
    private LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    /** Combina fecha y hora de inicio del evento. */
    private LocalDateTime fechaHoraEvento(Event event) {
        return event.getEventDate().atTime(event.getEventTime());
    }

    /** Pasa a active los draft cuya hora de inicio ya llegó; persiste cambios. */
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

    /** Pasa a finished los active transcurridas 2 h desde el inicio. */
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

    /** Borra finished (y sus asistencias) 24 h después de finalizar. */
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
            borrarEventoConAsistencias(evento);
            contador++;
            log.info("[EventService] Evento eliminado: '{}' (Fecha: {}, Hora: {})",
                    evento.getName(), evento.getEventDate(), evento.getEventTime());
        }

        log.info("[EventService] Total eventos eliminados: {}", contador);
        return contador;
    }

    /** Borra cancelled 2 h después de la cancelación. */
    @Transactional
    public int eliminarEventosCancelados() {
        log.info("[EventService] Eliminando eventos cancelados (>2h después de cancelar)");

        LocalDateTime ahora = ahora();
        List<Event> eventosAEliminar = eventRepository.findByStatus(EventStatus.cancelled).stream()
                .filter(evento -> canceladoDebeDesaparecer(evento, ahora))
                .toList();

        if (eventosAEliminar.isEmpty()) {
            log.info("[EventService] No hay eventos cancelados para eliminar");
            return 0;
        }

        log.info("[EventService] Eventos cancelados encontrados para eliminar: {}", eventosAEliminar.size());

        int contador = 0;
        for (Event evento : eventosAEliminar) {
            borrarEventoConAsistencias(evento);
            contador++;
            log.info("[EventService] Evento cancelado eliminado: '{}' (Cancelado en: {})",
                    evento.getName(), evento.getCancelledAt());
        }

        log.info("[EventService] Total eventos cancelados eliminados: {}", contador);
        return contador;
    }

    /** True si el cancelado ya cumplió 2 h (o no tiene marca, legado). */
    private boolean canceladoDebeDesaparecer(Event evento, LocalDateTime ahora) {
        LocalDateTime canceladoEn = evento.getCancelledAt();
        if (canceladoEn == null) {
            return true;
        }
        return !canceladoEn.plusHours(HORAS_DESPUES).isAfter(ahora);
    }

    /** Borra asistencias y luego el evento (inscripciones/espera caen por FK). */
    private void borrarEventoConAsistencias(Event evento) {
        eventAttendanceRepository.deleteAll(
                eventAttendanceRepository.findByRegistration_EventId(evento.getId()));
        eventRepository.delete(evento);
    }

    /** Encadena activar, finalizar y eliminar eventos expirados o cancelados. */
    @Transactional
    public void procesarEstadosEventos() {
        log.info("[EventService] Procesando estados de eventos");

        int activados = activarEventos();
        int finalizados = finalizarEventos();
        int cancelados = eliminarEventosCancelados();
        int eliminados = eliminarEventosExpirados();

        if (activados > 0 || finalizados > 0 || cancelados > 0 || eliminados > 0) {
            log.info("[EventService] Resumen: Activados: {}, Finalizados: {}, Cancelados: {}, Eliminados: {}",
                    activados, finalizados, cancelados, eliminados);
        }
    }

    /** Cada minuto aplica transiciones para que los cancelados desaparezcan a las 2 h. */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void procesarEstadosEventosProgramado() {
        procesarEstadosEventos();
    }

    /** Convierte el evento a DTO de detalle, con portada por defecto si falta. */
    private EventResponse convertToResponse(Event event) {
        Sport sport = sportRepository.findById(event.getSportId()).orElse(null);
        String sportName = sport != null ? sport.getName() : "N/A";
        String imageUrl = event.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = EventImageDefaults.forSport(sportName);
        }
        return EventResponse.builder()
                .id(event.getId())
                .sportId(event.getSportId())
                .sportName(sportName)
                .name(event.getName())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventTime(event.getEventTime())
                .location(event.getLocation())
                .imageUrl(imageUrl)
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .maxCapacity(event.getMaxCapacity())
                .availableCapacity(event.getAvailableCapacity())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .cancelledAt(event.getCancelledAt())
                .build();
    }

    /** Convierte el evento a DTO de calendario. */
    private CalendarEventResponse convertToCalendarResponse(Event event) {
        Sport sport = sportRepository.findById(event.getSportId()).orElse(null);
        return CalendarEventResponse.builder()
                .id(event.getId())
                .title(event.getName())
                .startDate(event.getEventDate())
                .startTime(event.getEventTime())
                .location(event.getLocation())
                .sportName(sport != null ? sport.getName() : "N/A")
                .availableCapacity(event.getAvailableCapacity())
                .maxCapacity(event.getMaxCapacity())
                .build();
    }
}
