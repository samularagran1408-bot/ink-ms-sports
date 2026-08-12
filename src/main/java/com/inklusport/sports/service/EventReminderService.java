package com.inklusport.sports.service;

import com.inklusport.sports.entity.Event;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final StaffNotificationService staffNotificationService;

    /**
     * Ejecuta cada minuto y envía recordatorios para eventos que comienzan dentro de 2 horas.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendEventReminders() {
        sendEventReminders(LocalDateTime.now());
    }

    @Transactional
    void sendEventReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusHours(2);

        log.info("Buscando eventos que comienzan entre {} y {}", now, windowEnd);

        List<Event> events = eventRepository.findByStatus(EventStatus.active).stream()
                .filter(event -> {
                    LocalDateTime eventStart = LocalDateTime.of(event.getEventDate(), event.getEventTime());
                    return !eventStart.isBefore(now) && !eventStart.isAfter(windowEnd);
                })
                .toList();

        if (events.isEmpty()) {
            log.info("No hay eventos programados para las próximas 2 horas");
            return;
        }

        log.info("Encontrados {} eventos para las próximas 2 horas", events.size());

        for (Event event : events) {
            sendRemindersForEvent(event);
        }
    }

    private void sendRemindersForEvent(Event event) {
        List<EventRegistration> registrations = registrationRepository
                .findByEventIdAndWaitlistPositionIsNullAndReminderSentAtIsNull(event.getId());

        if (registrations.isEmpty()) {
            log.info("No hay inscritos pendientes de recordatorio para el evento: {}", event.getName());
            return;
        }

        log.info("Enviando recordatorios para '{}' a {} usuarios inscritos", event.getName(), registrations.size());

        for (EventRegistration registration : registrations) {
            staffNotificationService.notifyUser(
                    registration.getUserId(),
                    "event_reminder",
                    "Recordatorio: Tu evento empieza en 2 horas",
                    String.format(
                            "Recuerda que tu evento '%s' comienza a las %s en %s. Falta poco para que empiece. ¡Te esperamos!",
                            event.getName(),
                            event.getEventTime().toString(),
                            event.getLocation() != null ? event.getLocation() : "nuestra sede"
                    ),
                    event.getId()
            );
            registration.setReminderSentAt(LocalDateTime.now());
            registrationRepository.save(registration);
        }

        staffNotificationService.notifyOrganizer(
                event.getCreatedBy(),
                "organizer_event_reminder",
                "Tu evento empieza en 2 horas",
                String.format(
                        "El evento '%s' comienza a las %s en %s. Hay %d inscritos confirmados.",
                        event.getName(),
                        event.getEventTime(),
                        event.getLocation() != null ? event.getLocation() : "sede por confirmar",
                        registrations.size()
                ),
                event.getId()
        );
    }
}
