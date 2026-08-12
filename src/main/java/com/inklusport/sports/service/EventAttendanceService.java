package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.AttendanceReportResponse;
import com.inklusport.sports.dto.AttendanceRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Registra asistencia manual/admin; exige quiz de staff (organizador o entrenador).
     */
    @Transactional
    public String recordAttendance(AttendanceRequest request) {
        quizEligibilityService.assertCurrentStaffQuizPassed();
        return recordAttendanceInternal(
                request.getRegistrationId(),
                request.getCheckInMethod(),
                request.getVerifiedBy()
        );
    }

    /**
     * Check-in por código QR de la inscripción (sin quiz de staff).
     */
    @Transactional
    public String recordAttendanceByQr(String qrCode, String verifiedBy) {
        EventRegistration registration = resolveRegistrationByQr(qrCode);
        return recordAttendanceInternal(registration.getId(), CheckInMethod.qr.name(), verifiedBy);
    }

    @Transactional(readOnly = true)
    public QrAttendanceInfoResponse getQrInfo(String rawQrCode) {
        EventRegistration registration = resolveRegistrationByQr(rawQrCode);
        Event event = eventRepository.findById(registration.getEventId()).orElse(null);

        boolean owned = false;
        try {
            String principal = userIdentityService.currentPrincipal();
            if (principal != null) {
                owned = userIdentityService.identityAliases(principal)
                        .contains(registration.getUserId());
            }
        } catch (Exception e) {
            log.debug("No se pudo resolver dueño del QR {}: {}", registration.getId(), e.getMessage());
        }

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

    private String recordAttendanceInternal(String registrationId, String checkInMethod, String verifiedBy) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error: La inscripción con ID '" + registrationId + "' no existe."
                ));

        if (registration.getWaitlistPosition() != null) {
            throw new IllegalStateException(
                    "Error: El usuario aún está en lista de espera; no se puede registrar asistencia."
            );
        }

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

        return "Asistencia confirmada exitosamente. Código de registro: " + saved.getId();
    }

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
                    .checkInTime(attendance.getCheckInTime())
                    .checkInMethod(attendance.getCheckInMethod() != null
                            ? attendance.getCheckInMethod().name()
                            : null)
                    .verifiedBy(attendance.getVerifiedBy())
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

    private UserNames resolveUserNames(String userId) {
        if (userId == null) {
            return new UserNames(null, null);
        }
        try {
            Map<String, Object> user = userServiceClient.getUserByIdInternal(userId);
            return new UserNames(
                    stringField(user, "fullName", "full_name", "name"),
                    stringField(user, "email")
            );
        } catch (Exception e) {
            log.warn("No se pudo enriquecer usuario {} para el reporte: {}", userId, e.getMessage());
            return new UserNames(null, null);
        }
    }

    private record UserNames(String fullName, String email) {}

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

    @Transactional(readOnly = true)
    public List<EventAttendance> getAllAttendances() {
        log.info("Obteniendo listado global de asistencias");
        return eventAttendanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByEvent(String eventId) {
        log.info("Obteniendo asistencias para el evento con ID: {}", eventId);
        return eventAttendanceRepository.findByRegistration_EventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByRegistration(String registrationId) {
        log.info("Obteniendo asistencias para la inscripción con ID: {}", registrationId);
        return eventAttendanceRepository.findByRegistrationId(registrationId);
    }
}
