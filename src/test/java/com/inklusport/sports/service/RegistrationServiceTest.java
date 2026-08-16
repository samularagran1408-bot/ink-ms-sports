package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.entity.EventRegistration;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldNotifyFirstWaitlistUserWhenConfirmedRegistrationIsCanceled() {
        EventRegistration canceledRegistration = EventRegistration.builder()
                .id("reg-confirmed")
                .eventId("event-1")
                .userId("registered-user")
                .waitlistPosition(null)
                .build();

        EventRegistration promotedRegistration = EventRegistration.builder()
                .id("reg-waitlist-1")
                .eventId("event-1")
                .userId("waitlist-user-1")
                .waitlistPosition(1)
                .build();

        when(registrationRepository.findById("reg-confirmed"))
                .thenReturn(Optional.of(canceledRegistration));
        when(registrationRepository.findFirstByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc("event-1"))
                .thenReturn(Optional.of(promotedRegistration))
                .thenReturn(Optional.empty());
        when(registrationRepository.save(promotedRegistration)).thenReturn(promotedRegistration);
        when(registrationRepository.findByEventIdAndWaitlistPositionIsNotNullOrderByWaitlistPositionAsc("event-1"))
                .thenReturn(java.util.Collections.emptyList());

        registrationService.cancelRegistration("reg-confirmed");

        verify(staffNotificationService, times(1)).notifyUser(
                eq("waitlist-user-1"),
                eq("waitlist_promoted"),
                anyString(),
                anyString(),
                eq("event-1")
        );
    }
}
