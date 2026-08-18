package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.FutureRegistrationsCheckResponse;
import com.inklusport.sports.dto.RegistrationRequest;
import com.inklusport.sports.dto.RegistrationResponse;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final StaffNotificationService staffNotificationService;
    private final UserIdentityService userIdentityService;
    private final UserServiceClient userServiceClient;

    @Transactional
    public RegistrationResponse registerToEvent(RegistrationRequest request) {
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (event.getStatus() == EventStatus.cancelled) {
            throw new IllegalStateException("No es posible inscribirse a un evento cancelado.");
        }
        if (event.getStatus() == EventStatus.finished) {
            throw new IllegalStateException("No es posible inscribirse a un evento finalizado.");
        }

        String userId = userIdentityService.resolveCanonicalUserId(request.getUserId());
        Set<String> aliases = userIdentityService.identityAliases(userId);
        aliases.add(userId);
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            aliases.add(request.getUserId().trim());
        }

        boolean alreadyRegistered = aliases.stream()
                .anyMatch(alias -> registrationRepository.existsByEventIdAndUserId(request.getEventId(), alias));
        if (alreadyRegistered) {
            throw new IllegalStateException("El usuario ya se encuentra registrado.");
        }

        String notifyTarget = userIdentityService.currentPrincipal() != null
                ? userIdentityService.currentPrincipal()
                : userId;

        EventRegistration registration = new EventRegistration();
        registration.setId(UUID.randomUUID().toString());
        registration.setEventId(request.getEventId());
        registration.setUserId(userId);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setAttended(false);
        registration.setQrCode("QR_" + UUID.randomUUID());

        String statusMessage;
        String notificationType;
        String notificationTitle;
        String notificationBody;
        boolean confirmed;

        Integer available = event.getAvailableCapacity();
        if (available == null) {
            available = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;
            event.setAvailableCapacity(available);
        }

        if (available > 0) {
            registration.setWaitlistPosition(null);
            event.setAvailableCapacity(available - 1);
            eventRepository.saveAndFlush(event);

            statusMessage = "Inscripción confirmada exitosamente. ¡Cupo asegurado!";
            notificationType = "event_registration";
            notificationTitle = "¡Inscripción confirmada!";
            notificationBody = "Te has inscrito correctamente al evento: " + event.getName();
            confirmed = true;
        } else {
            long personasEnEspera = registrationRepository.countByEventIdAndWaitlistPositionIsNotNull(request.getEventId());
            int nuevaPosicion = (int) personasEnEspera + 1;
            registration.setWaitlistPosition(nuevaPosicion);

            statusMessage = "El evento está lleno. Has sido agregado a la lista de espera en la posición: " + nuevaPosicion;
            notificationType = "waitlist_added";
            notificationTitle = "Lista de espera";
            notificationBody = "El evento " + event.getName() + " está lleno. Estás en la posición " + nuevaPosicion + " de la lista de espera.";
            confirmed = false;
        }

        EventRegistration saved = registrationRepository.saveAndFlush(registration);

        if (registration.getWaitlistPosition() != null && registration.getWaitlistPosition() == 1) {
            notifyNewWaitlistFirstPosition(request.getEventId(), saved);
        } else {
            sendNotification(notifyTarget, notificationType, notificationTitle, notificationBody, request.getEventId());
        }

        notifyOrganizerAboutRegistration(event, notifyTarget, confirmed, registration.getWaitlistPosition());

        if (confirmed && event.getAvailableCapacity() != null && event.getAvailableCapacity() == 0) {
            /**
             * Fan-out a admins incluido en notifyOrganizer
             */
            staffNotificationService.notifyOrganizer(
                    event.getCreatedBy(),
                    "event_full",
                    "Evento aforo completo",
                    "El evento \"" + event.getName() + "\" ya no tiene cupos disponibles.",
                    event.getId()
            );
        }

        return convertToResponse(saved, statusMessage, event, null, null);
    }

    @Transactional
    public void cancelRegistration(String registrationId) {
        EventRegistration currentReg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        String eventId = currentReg.getEventId();
        Integer posicionEliminada = currentReg.getWaitlistPosition();
        String cancelledUser = currentReg.getUserId();
        Event event = eventRepository.findById(eventId).orElse(null);

        registrationRepository.delete(currentReg);

        if (event != null) {
            staffNotificationService.notifyOrganizer(
                    event.getCreatedBy(),
                    "event_registration_cancelled",
                    "Inscripción cancelada",
                    "El usuario " + cancelledUser + " canceló su inscripción al evento \"" + event.getName() + "\".",
                    eventId
            );
        }

        if (posicionEliminada == null) {
            Optional<EventRegistration> nextInLine = registrationRepository
                    .findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(eventId);

            if (nextInLine.isPresent()) {
                EventRegistration promotedReg = nextInLine.get();
                promotedReg.setWaitlistPosition(null);
                registrationRepository.save(promotedReg);

                log.info("Usuario {} promovido automáticamente al evento.", promotedReg.getUserId());
                notifyPromotedWaitlistUser(eventId, promotedReg);

                reorderWaitlist(eventId);
                Optional<EventRegistration> newFirstAfterPromotion = registrationRepository
                        .findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(eventId);
                newFirstAfterPromotion.ifPresent(next -> notifyNewWaitlistFirstPosition(eventId, next));
            } else {
                Event ev = eventRepository.findById(eventId)
                        .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
                ev.setAvailableCapacity(ev.getAvailableCapacity() + 1);
                eventRepository.saveAndFlush(ev);
            }
        } else {
            if (posicionEliminada == 1) {
                Optional<EventRegistration> nextFirst = registrationRepository
                        .findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(eventId);
                nextFirst.ifPresent(next -> notifyNewWaitlistFirstPosition(eventId, next));
            }
            reorderWaitlist(eventId);
        }
    }

    private void notifyOrganizerAboutRegistration(Event event, String athleteEmail, boolean confirmed, Integer waitlistPos) {
        if (event.getCreatedBy() == null || event.getCreatedBy().isBlank()) {
            return;
        }
        if (confirmed) {
            staffNotificationService.notifyOrganizer(
                    event.getCreatedBy(),
                    "organizer_new_registration",
                    "Nueva inscripción en tu evento",
                    "El usuario " + athleteEmail + " se inscribió al evento \"" + event.getName() + "\". Cupos restantes: "
                            + event.getAvailableCapacity() + ".",
                    event.getId()
            );
        } else {
            staffNotificationService.notifyOrganizer(
                    event.getCreatedBy(),
                    "organizer_waitlist_joined",
                    "Nueva persona en lista de espera",
                    "El usuario " + athleteEmail + " entró a la lista de espera del evento \"" + event.getName()
                            + "\" (posición " + waitlistPos + ").",
                    event.getId()
            );
        }
    }

    private void sendNotification(String userId, String type, String title, String body, String eventId) {
        log.info("Enviando notificación - Usuario: {}, Título: {}", userId, title);
        staffNotificationService.notifyUser(userId, type, title, body, eventId);
    }

    @Transactional
    public void notifyPromotedWaitlistUser(String eventId, EventRegistration promotedReg) {
        String notificationType = "waitlist_promoted";
        String notificationTitle = "¡Ya estás inscrito al evento!";
        String notificationBody = "Felicidades. Has pasado de la lista de espera y ahora estás inscrito al evento: "
                + getEventName(eventId) + ". ¡Cupo asegurado!";

        sendNotification(promotedReg.getUserId(), notificationType, notificationTitle, notificationBody, eventId);

        eventRepository.findById(eventId).ifPresent(event ->
                staffNotificationService.notifyOrganizer(
                        event.getCreatedBy(),
                        "organizer_waitlist_promoted",
                        "Cupo asignado desde waitlist",
                        "El usuario " + promotedReg.getUserId() + " pasó de lista de espera a inscrito en \""
                                + event.getName() + "\".",
                        eventId
                )
        );
    }

    @Transactional
    public void notifyNewWaitlistFirstPosition(String eventId, EventRegistration newFirst) {
        String notificationType = "waitlist_position_update";
        String notificationTitle = "¡Avanzaste en la lista de espera!";
        String notificationBody = "Has pasado a la posición 1 en la lista de espera del evento: "
                + getEventName(eventId) + ". Si se libera un cupo, serás el siguiente en inscribirte.";

        sendNotification(newFirst.getUserId(), notificationType, notificationTitle, notificationBody, eventId);
    }

    @Transactional
    public void confirmWaitlistOffer(String userId, String eventId) {
        registrationRepository.updateWaitlistToConfirmed(userId, eventId);

        String notificationType = "waitlist_confirmed";
        String notificationTitle = "¡Cupo confirmado!";
        String notificationBody = "Has confirmado tu asistencia al evento. ¡Te esperamos!";

        sendNotification(userId, notificationType, notificationTitle, notificationBody, eventId);
    }

    private String getEventName(String eventId) {
        return eventRepository.findById(eventId)
                .map(Event::getName)
                .orElse("el evento");
    }

    private void reorderWaitlist(String eventId) {
        List<EventRegistration> waitlist = registrationRepository
                .findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(eventId);

        int currentPosition = 1;
        for (EventRegistration reg : waitlist) {
            reg.setWaitlistPosition(currentPosition);
            registrationRepository.save(reg);
            currentPosition++;
        }
    }

    public List<RegistrationResponse> getWaitlistForEvent(String eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        List<EventRegistration> waitlist = registrationRepository
                .findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc(eventId);

        return waitlist.stream()
                .map(reg -> {
                    UserNames names = resolveUserNames(reg.getUserId());
                    return convertToResponse(reg, "WAITLIST", event, names.fullName, names.email);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrationsByUser(String userId) {
        Set<String> aliases = userIdentityService.identityAliases(userId);
        Map<String, EventRegistration> unique = new LinkedHashMap<>();
        for (String alias : aliases) {
            for (EventRegistration reg : registrationRepository.findByUserId(alias)) {
                unique.putIfAbsent(reg.getId(), reg);
            }
        }

        List<RegistrationResponse> responses = new ArrayList<>();
        for (EventRegistration reg : unique.values()) {
            Event event = eventRepository.findById(reg.getEventId()).orElse(null);
            String status = reg.getWaitlistPosition() != null ? "WAITLIST" : "CONFIRMED";
            responses.add(convertToResponse(reg, status, event, null, null));
        }
        responses.sort(Comparator
                .comparing((RegistrationResponse r) -> r.getEventDate() == null ? LocalDate.MIN : r.getEventDate())
                .thenComparing(r -> r.getEventTime() == null ? LocalTime.MIN : r.getEventTime())
                .reversed());
        return responses;
    }

    @Transactional(readOnly = true)
    public FutureRegistrationsCheckResponse checkFutureRegistrations(String userId) {
        Set<String> aliases = userIdentityService.identityAliases(userId);
        aliases.add(userId);

        Map<String, EventRegistration> unique = new LinkedHashMap<>();
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            for (EventRegistration reg : registrationRepository.findByUserId(alias)) {
                unique.putIfAbsent(reg.getId(), reg);
            }
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<String> eventNames = new ArrayList<>();
        for (EventRegistration reg : unique.values()) {
            Event event = eventRepository.findById(reg.getEventId()).orElse(null);
            if (event == null || event.getEventDate() == null) {
                continue;
            }
            if (event.getStatus() == EventStatus.finished || event.getStatus() == EventStatus.cancelled) {
                continue;
            }
            boolean future = event.getEventDate().isAfter(today)
                    || (event.getEventDate().isEqual(today)
                    && (event.getEventTime() == null || !event.getEventTime().isBefore(now)));
            if (future) {
                eventNames.add(event.getName());
            }
        }

        return FutureRegistrationsCheckResponse.builder()
                .hasFutureRegistrations(!eventNames.isEmpty())
                .count(eventNames.size())
                .eventNames(eventNames)
                .build();
    }

    private RegistrationResponse convertToResponse(EventRegistration reg, String statusMessage, Event event,
                                                   String userFullName, String userEmail) {
        return RegistrationResponse.builder()
                .id(reg.getId())
                .userId(reg.getUserId())
                .userFullName(userFullName)
                .userEmail(userEmail)
                .eventId(reg.getEventId())
                .eventName(event != null ? event.getName() : "Evento")
                .eventDate(event != null ? event.getEventDate() : null)
                .eventTime(event != null ? event.getEventTime() : null)
                .eventStatus(event != null && event.getStatus() != null ? event.getStatus().name() : null)
                .qrCode(reg.getQrCode())
                .registrationDate(reg.getRegistrationDate())
                .attended(reg.getAttended())
                .waitlistPosition(reg.getWaitlistPosition())
                .message(statusMessage)
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
            log.debug("No se pudo enriquecer usuario {} de waitlist: {}", userId, e.getMessage());
            return new UserNames(null, null);
        }
    }

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

    private record UserNames(String fullName, String email) {}
}
