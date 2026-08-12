package com.inklusport.sports.service;

import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private StaffNotificationService staffNotificationService;

    @InjectMocks
    private EventReminderService eventReminderService;

    @Test
    void shouldSendReminderOnlyForEventsWithinTwoHours() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);

        Event dueEvent = Event.builder()
                .id("event-1")
                .name("Taller de movilidad")
                .eventDate(LocalDate.of(2026, 7, 1))
                .eventTime(LocalTime.of(10, 0))
                .location("Sede central")
                .status(EventStatus.active)
                .createdBy("org-1")
                .maxCapacity(20)
                .availableCapacity(20)
                .build();

        Event tooFarEvent = Event.builder()
                .id("event-2")
                .name("Evento futuro")
                .eventDate(LocalDate.of(2026, 7, 1))
                .eventTime(LocalTime.of(12, 0))
                .location("Sede central")
                .status(EventStatus.active)
                .maxCapacity(20)
                .availableCapacity(20)
                .build();

        EventRegistration confirmedRegistration = EventRegistration.builder()
                .id("reg-1")
                .userId("user-1")
                .eventId("event-1")
                .waitlistPosition(null)
                .build();

        when(eventRepository.findByStatus(EventStatus.active)).thenReturn(List.of(dueEvent, tooFarEvent));
        when(registrationRepository.findByEventIdAndWaitlistPositionIsNullAndReminderSentAtIsNull("event-1"))
                .thenReturn(List.of(confirmedRegistration));

        eventReminderService.sendEventReminders(now);

        verify(staffNotificationService).notifyUser(
                eq("user-1"),
                eq("event_reminder"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService).notifyOrganizer(
                eq("org-1"),
                eq("organizer_event_reminder"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService, never()).notifyUser(
                eq("user-2"),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldNotSendDuplicateReminders() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);

        Event dueEvent = Event.builder()
                .id("event-1")
                .name("Taller de movilidad")
                .eventDate(LocalDate.of(2026, 7, 1))
                .eventTime(LocalTime.of(10, 0))
                .location("Sede central")
                .status(EventStatus.active)
                .maxCapacity(20)
                .availableCapacity(20)
                .build();

        when(eventRepository.findByStatus(EventStatus.active)).thenReturn(List.of(dueEvent));
        when(registrationRepository.findByEventIdAndWaitlistPositionIsNullAndReminderSentAtIsNull("event-1"))
                .thenReturn(List.of());

        eventReminderService.sendEventReminders(now);

        verify(staffNotificationService, never()).notifyUser(any(), any(), any(), any(), any());
        verify(staffNotificationService, never()).notifyOrganizer(any(), any(), any(), any(), any());
    }
}
