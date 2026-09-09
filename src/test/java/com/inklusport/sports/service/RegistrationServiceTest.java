package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StaffNotificationService staffNotificationService;

    @Mock
    private UserIdentityService userIdentityService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldPromoteFirstWaitlistUserAndNotifyEveryoneWhoMovedUp() {
        Event event = Event.builder()
                .id("event-1")
                .name("Natación adaptada")
                .createdBy("organizer-1")
                .status(EventStatus.active)
                .availableCapacity(0)
                .build();

        EventRegistration canceledRegistration = EventRegistration.builder()
                .id("reg-confirmed")
                .eventId("event-1")
                .userId("registered-user")
                .waitlistPosition(null)
                .attended(false)
                .build();

        EventRegistration promotedRegistration = EventRegistration.builder()
                .id("reg-waitlist-1")
                .eventId("event-1")
                .userId("waitlist-user-1")
                .waitlistPosition(1)
                .build();

        EventRegistration secondInLine = EventRegistration.builder()
                .id("reg-waitlist-2")
                .eventId("event-1")
                .userId("waitlist-user-2")
                .waitlistPosition(2)
                .build();

        EventRegistration thirdInLine = EventRegistration.builder()
                .id("reg-waitlist-3")
                .eventId("event-1")
                .userId("waitlist-user-3")
                .waitlistPosition(3)
                .build();

        when(registrationRepository.findById("reg-confirmed"))
                .thenReturn(Optional.of(canceledRegistration));
        when(eventRepository.findByIdForUpdate("event-1")).thenReturn(Optional.of(event));
        when(registrationRepository.findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc("event-1"))
                .thenReturn(Optional.of(promotedRegistration));
        when(registrationRepository.save(promotedRegistration)).thenReturn(promotedRegistration);
        when(registrationRepository.findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc("event-1"))
                .thenReturn(List.of(secondInLine, thirdInLine));

        registrationService.cancelRegistration("reg-confirmed");

        verify(staffNotificationService, times(1)).notifyUser(
                eq("waitlist-user-1"),
                eq("waitlist_promoted"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService, times(1)).notifyUser(
                eq("waitlist-user-2"),
                eq("waitlist_position_update"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService, times(1)).notifyUser(
                eq("waitlist-user-3"),
                eq("waitlist_position_update"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService, never()).notifyUser(
                eq("waitlist-user-1"),
                eq("waitlist_position_update"),
                anyString(),
                anyString(),
                eq("event-1")
        );
    }

    @Test
    void shouldNotifyRemainingWaitlistWhenSomeoneLeavesTheQueue() {
        Event event = Event.builder()
                .id("event-1")
                .name("Natación adaptada")
                .createdBy("organizer-1")
                .status(EventStatus.active)
                .build();

        EventRegistration leavingWaitlist = EventRegistration.builder()
                .id("reg-waitlist-1")
                .eventId("event-1")
                .userId("waitlist-user-1")
                .waitlistPosition(1)
                .attended(false)
                .build();

        EventRegistration secondInLine = EventRegistration.builder()
                .id("reg-waitlist-2")
                .eventId("event-1")
                .userId("waitlist-user-2")
                .waitlistPosition(2)
                .build();

        when(registrationRepository.findById("reg-waitlist-1"))
                .thenReturn(Optional.of(leavingWaitlist));
        when(eventRepository.findByIdForUpdate("event-1")).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc("event-1"))
                .thenReturn(List.of(secondInLine));

        registrationService.cancelRegistration("reg-waitlist-1");

        verify(staffNotificationService, never()).notifyUser(
                eq("waitlist-user-2"),
                eq("waitlist_promoted"),
                anyString(),
                anyString(),
                eq("event-1")
        );
        verify(staffNotificationService, times(1)).notifyUser(
                eq("waitlist-user-2"),
                eq("waitlist_position_update"),
                anyString(),
                anyString(),
                eq("event-1")
        );
    }
}
