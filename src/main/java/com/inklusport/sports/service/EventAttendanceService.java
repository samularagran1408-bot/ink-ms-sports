package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.AttendanceReportResponse;
import com.inklusport.sports.dto.AttendanceRequest;
import com.inklusport.sports.dto.BulkAttendanceRequest;
import com.inklusport.sports.dto.QrAttendanceInfoResponse;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.EventAttendance;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.enums.CheckInMethod;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.util.QrCodeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registro de asistencia a eventos (manual, QR y masivo). */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventAttendanceService {

    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRepository eventRepository;
    private final QuizEligibilityService quizEligibilityService;
    private final UserServiceClient userServiceClient;
    private final UserIdentityService userIdentityService;
    private final StaffNotificationService staffNotificationService;
    private final CompetitionModeService competitionModeService;

    /** Únicos roles que pueden usar el QR de otra persona. */
    private static final String[] ROLES_CHECKIN_AJENO = {"ADMIN", "ORGANIZER", "ORGANIZADOR"};

    /**
     * Registra asistencia manual/admin; exige quiz de staff (organizador o entrenador).
     */
    @Transactional
    public String recordAttendance(AttendanceRequest request) {
        quizEligibilityService.assertCurrentStaffQuizPassed();
        return recordAttendanceInternal(
                request.getRegistrationId(),
                request.getCheckInMethod(),
                request.getVerifiedBy(),
                request.getNotes()
        );
    }

    /**
     * Check-in por código QR de la inscripción (sin quiz de staff).
     */
    @Transactional
    public String recordAttendanceByQr(String qrCode, String verifiedBy, String notes) {
        EventRegistration registration = resolveRegistrationByQr(qrCode);
        assertPuedeUsarQr(registration);
        return recordAttendanceInternal(registration.getId(), CheckInMethod.qr.name(), verifiedBy, notes);
    }

    /** Datos de la inscripción a partir del QR, incluyendo si pertenece al usuario actual. */
    @Transactional(readOnly = true)
    public QrAttendanceInfoResponse getQrInfo(String rawQrCode) {
        EventRegistration registration = resolveRegistrationByQr(rawQrCode);
        assertPuedeUsarQr(registration);
        Event event = eventRepository.findById(registration.getEventId()).orElse(null);

        boolean owned = esDelUsuarioActual(registration);

        return QrAttendanceInfoResponse.builder()
                .qrCode(registration.getQrCode())
                .registrationId(registration.getId())
                .eventId(registration.getEventId())
                .eventName(event != null ? event.getName() : null)
                .eventDate(event != null && event.getEventDate() != null ? event.getEventDate().toString() : null)
                .eventTime(event != null && event.getEventTime() != null ? event.getEventTime().toString() : null)
                .location(event != null ? event.getLocation() : null)
                .attended(Boolean.TRUE.equals(registration.getAttended()))
                .ownedByCurrentUser(owned)
                .build();
    }

    /**
     * El QR es personal: sólo lo usa su dueño, un admin o un organizador. Antes
     * bastaba con estar autenticado, así que cualquier usuario con sesión podía
     * consultar o registrar la asistencia de otra persona con su código.
     */
    private void assertPuedeUsarQr(EventRegistration registration) {
        if (userIdentityService.hasAnyRole(ROLES_CHECKIN_AJENO)) {
            return;
        }
        if (esDelUsuarioActual(registration)) {
            return;
        }
        throw new AccessDeniedException(
                "Este código QR pertenece a otra persona. Inicia sesión con esa cuenta "
                        + "o pide a un administrador u organizador que registre la asistencia."
        );
    }

    /** True si la inscripción es del usuario autenticado (UUID o email). */
    private boolean esDelUsuarioActual(EventRegistration registration) {
        try {
            String principal = userIdentityService.currentPrincipal();
            if (principal == null) {
                return false;
            }
            return userIdentityService.identityAliases(principal).stream()
                    .anyMatch(alias -> alias != null && alias.equalsIgnoreCase(registration.getUserId()));
        } catch (Exception e) {
            log.debug("No se pudo resolver dueño del QR {}: {}", registration.getId(), e.getMessage());
            return false;
        }
    }

    /** Extrae el código QR y resuelve la inscripción; lanza si es inválido. */
    private EventRegistration resolveRegistrationByQr(String rawQrCode) {
        String qrCode = QrCodeParser.extract(rawQrCode);
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalArgumentException("Error: Debes enviar un código QR válido.");
        }

        return eventRegistrationRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error: Código QR no válido o inscripción no encontrada."
                ));
    }

    /** Persiste el check-in, marca asistencia y notifica; lanza si no aplica. */
    private String recordAttendanceInternal(
            String registrationId,
            String checkInMethod,
            String verifiedBy,
            String notes
    ) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error: La inscripción con ID '" + registrationId + "' no existe."
                ));

        if (registration.getWaitlistPosition() != null) {
            throw new IllegalStateException(
                    "Error: El usuario aún está en lista de espera; no se puede registrar asistencia."
            );
        }

        Event event = assertEventHasStarted(registration);

        if (eventAttendanceRepository.existsByRegistrationId(registrationId)) {
            throw new IllegalStateException(
                    "Error: Ya se registró la asistencia para esta inscripción previamente."
            );
        }

        CheckInMethod method;
        try {
            method = CheckInMethod.valueOf(
                    checkInMethod == null ? CheckInMethod.qr.name() : checkInMethod.toLowerCase().trim()
            );
        } catch (Exception e) {
            log.warn("Método de check-in inválido recibido: {}. Se usará el valor por defecto.", checkInMethod);
            method = CheckInMethod.qr;
        }

        EventAttendance attendance = EventAttendance.builder()
                .registrationId(registrationId)
                .checkInMethod(method)
                .verifiedBy(verifiedBy)
                .notes(normalizeNotes(notes))
                .build();

        EventAttendance saved = eventAttendanceRepository.save(attendance);
        log.info("Asistencia registrada exitosamente con ID: {}", saved.getId());

        boolean alreadyMarked = Boolean.TRUE.equals(registration.getAttended());
        registration.setAttended(true);
        eventRegistrationRepository.save(registration);

        if (!alreadyMarked) {
            try {
                userServiceClient.incrementEventsAttended(registration.getUserId());
            } catch (Exception e) {
                log.warn(
                        "No se pudo incrementar events_attended para {}: {}",
                        registration.getUserId(),
                        e.getMessage()
                );
            }
        }

        UserNames atleta = resolveUserNames(registration.getUserId());
        notifyAttendance(event, registration, atleta);
        sumarProgresoCompetencia(event, registration, atleta.email());
        return "Asistencia confirmada exitosamente. Código de registro: " + saved.getId();
    }

    /**
     * Pide al asistente que el check-in sume al plan de competencia del atleta.
     *
     * <p>El token se lee aquí, en el hilo de la petición, porque el envío sale
     * en otro hilo para no retrasar la respuesta del check-in.
     */
    private void sumarProgresoCompetencia(Event event, EventRegistration registration, String athleteEmail) {
        try {
            competitionModeService.registerEventProgress(
                    registration.getUserId(),
                    athleteEmail,
                    registration.getEventId(),
                    event != null ? event.getName() : null,
                    currentAuthorization()
            );
        } catch (Exception e) {
            log.warn("No se pudo encolar el progreso del check-in {}: {}", registration.getId(), e.getMessage());
        }
    }

    /** Cabecera Authorization de la petición en curso, o null si no hay. */
    private String currentAuthorization() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }

    /** Notifica al atleta y al organizador (o admins) el check-in registrado. */
    private void notifyAttendance(Event event, EventRegistration registration, UserNames names) {
        try {
            String eventName = event != null && event.getName() != null ? event.getName() : "un evento";
            String eventId = registration.getEventId();
            String athlete = names.fullName != null && !names.fullName.isBlank()
                    ? names.fullName
                    : (names.email != null ? names.email : "Un atleta");

            staffNotificationService.notifyUser(
                    registration.getUserId(),
                    "attendance_confirmed",
                    "Asistencia confirmada",
                    "Tu check-in en " + eventName + " quedó registrado.",
                    eventId
            );
            if (event != null && event.getCreatedBy() != null && !event.getCreatedBy().isBlank()) {
                staffNotificationService.notifyOrganizer(
                        event.getCreatedBy(),
                        "attendance_checkin",
                        "Nuevo check-in",
                        athlete + " registró asistencia en " + eventName + ".",
                        eventId
                );
            } else {
                staffNotificationService.notifyAdmins(
                        "attendance_checkin",
                        "Nuevo check-in",
                        athlete + " registró asistencia en " + eventName + ".",
                        eventId
                );
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar el check-in de {}: {}", registration.getId(), e.getMessage());
        }
    }

    /** Valida que el evento exista y ya haya comenzado; lanza si es prematuro. */
    private Event assertEventHasStarted(EventRegistration registration) {
        if (registration.getEventId() == null || registration.getEventId().isBlank()) {
            throw new IllegalArgumentException("Error: La inscripción no tiene evento asociado.");
        }
        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error: El evento asociado a la inscripción no existe."
                ));
        if (event.getEventDate() == null || event.getEventTime() == null) {
            return event;
        }
        LocalDateTime start = LocalDateTime.of(event.getEventDate(), event.getEventTime());
        if (LocalDateTime.now().isBefore(start)) {
            throw new IllegalStateException(
                    "Error: El registro de asistencia se habilita a partir de "
                            + event.getEventDate() + " " + event.getEventTime() + "."
            );
        }
        return event;
    }

    /**
     * Check-in masivo: una sola validación de quiz y un resultado parcial por inscripción.
     */
    public Map<String, Object> recordBulkAttendance(BulkAttendanceRequest request) {
        quizEligibilityService.assertCurrentStaffQuizPassed();

        List<String> ids = request.getRegistrationIds() == null ? List.of() : request.getRegistrationIds();
        String method = request.getCheckInMethod() == null ? CheckInMethod.admin.name() : request.getCheckInMethod();
        String verifiedBy = request.getVerifiedBy();
        String notes = request.getNotes();

        int succeeded = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (String registrationId : ids) {
            if (registrationId == null || registrationId.isBlank()) {
                continue;
            }
            try {
                recordAttendanceInternal(registrationId.trim(), method, verifiedBy, notes);
                succeeded++;
            } catch (Exception e) {
                failed++;
                errors.add(registrationId + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", failed == 0 ? "SUCCESS" : "PARTIAL");
        result.put("message", "Asistencias registradas: " + succeeded + ". Fallidas: " + failed + ".");
        result.put("succeeded", succeeded);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    /** Arma el reporte de asistentes y ausentes del evento; lanza si no existe. */
    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Error: Debes indicar el eventId del reporte.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error: El evento con ID '" + eventId + "' no existe."
                ));

        List<EventRegistration> confirmed = eventRegistrationRepository
                .findByEventIdAndWaitlistPositionIsNull(eventId);
        List<EventAttendance> attendances = eventAttendanceRepository.findByRegistration_EventId(eventId);

        java.util.Set<String> attendedRegistrationIds = attendances.stream()
                .map(EventAttendance::getRegistrationId)
                .collect(java.util.stream.Collectors.toSet());

        List<AttendanceReportResponse.AttendeeRow> rows = new ArrayList<>();
        for (EventAttendance attendance : attendances) {
            EventRegistration registration = attendance.getRegistration();
            if (registration == null) {
                registration = eventRegistrationRepository.findById(attendance.getRegistrationId()).orElse(null);
            }

            String userId = registration != null ? registration.getUserId() : null;
            UserNames names = resolveUserNames(userId);

            rows.add(AttendanceReportResponse.AttendeeRow.builder()
                    .registrationId(attendance.getRegistrationId())
                    .userId(userId)
                    .fullName(names.fullName)
                    .email(names.email)
                    .profilePicture(names.profilePicture)
                    .checkInTime(attendance.getCheckInTime())
                    .checkInMethod(attendance.getCheckInMethod() != null
                            ? attendance.getCheckInMethod().name()
                            : null)
                    .verifiedBy(attendance.getVerifiedBy())
                    .notes(attendance.getNotes())
                    .build());
        }

        List<AttendanceReportResponse.AbsentRow> absentees = new ArrayList<>();
        for (EventRegistration registration : confirmed) {
            if (attendedRegistrationIds.contains(registration.getId())) {
                continue;
            }
            UserNames names = resolveUserNames(registration.getUserId());
            absentees.add(AttendanceReportResponse.AbsentRow.builder()
                    .registrationId(registration.getId())
                    .userId(registration.getUserId())
                    .fullName(names.fullName)
                    .email(names.email)
                    .profilePicture(names.profilePicture)
                    .build());
        }

        long totalRegistered = confirmed.size();
        long totalAttended = attendances.size();
        long totalAbsent = Math.max(totalRegistered - totalAttended, 0);
        double rate = totalRegistered == 0
                ? 0d
                : Math.round((totalAttended * 10000.0) / totalRegistered) / 100.0;

        return AttendanceReportResponse.builder()
                .eventId(eventId)
                .eventName(event.getName())
                .totalRegistered(totalRegistered)
                .totalAttended(totalAttended)
                .totalAbsent(totalAbsent)
                .attendanceRatePercent(rate)
                .attendees(rows)
                .absentees(absentees)
                .build();
    }

    /** Enriquece nombre, email y foto desde users-ms; no falla el flujo. */
    private UserNames resolveUserNames(String userId) {
        if (userId == null) {
            return new UserNames(null, null, null);
        }
        try {
            Map<String, Object> user = userServiceClient.getUserByIdInternal(userId);
            return new UserNames(
                    stringField(user, "fullName", "full_name", "name"),
                    stringField(user, "email"),
                    stringField(user, "profilePicture", "profile_picture")
            );
        } catch (Exception e) {
            log.warn("No se pudo enriquecer usuario {} para el reporte: {}", userId, e.getMessage());
            return new UserNames(null, null, null);
        }
    }

    /** Nombre, email y foto de perfil para reportes de asistencia. */
    private record UserNames(String fullName, String email, String profilePicture) {}

    /** Recorta y limita el comentario de asistencia; vacío → null. */
    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    /** Primer valor no vacío entre las claves indicadas del mapa. */
    private String stringField(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    /** Lista global de registros de asistencia. */
    @Transactional(readOnly = true)
    public List<EventAttendance> getAllAttendances() {
        log.info("Obteniendo listado global de asistencias");
        return eventAttendanceRepository.findAll();
    }

    /** Lista asistencias asociadas a un evento. */
    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByEvent(String eventId) {
        log.info("Obteniendo asistencias para el evento con ID: {}", eventId);
        return eventAttendanceRepository.findByRegistration_EventId(eventId);
    }

    /** Lista asistencias de una inscripción. */
    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByRegistration(String registrationId) {
        log.info("Obteniendo asistencias para la inscripción con ID: {}", registrationId);
        return eventAttendanceRepository.findByRegistrationId(registrationId);
    }
}
