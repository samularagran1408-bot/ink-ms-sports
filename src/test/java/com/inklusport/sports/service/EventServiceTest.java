package com.inklusport.sports.service;

import com.inklusport.sports.dto.CalendarEventResponse;
import com.inklusport.sports.dto.EventRequest;
import com.inklusport.sports.dto.EventUpdateRequest;
import com.inklusport.sports.entity.Event;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.enums.EventStatus;
import com.inklusport.sports.exception.ResourceNotFoundException;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import com.inklusport.sports.repository.EventRepository;
import com.inklusport.sports.repository.SportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private SportRepository sportRepository;
    @Mock
    private EventAttendanceRepository eventAttendanceRepository;
    @Mock
    private EventRegistrationRepository eventRegistrationRepository;
    @Mock
    private StaffNotificationService staffNotificationService;
    @Mock
    private QuizEligibilityService quizEligibilityService;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventRejectsExceededCapacity() {
        EventRequest request = new EventRequest();
        request.setSportId(1);
        request.setName("Torneo");
        request.setMaxCapacity(501);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> eventService.createEvent(request)
        );
        assertTrue(error.getMessage().contains("cupo del evento está excedido"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void getEventByIdThrowsWhenMissing() {
        when(eventRepository.findById("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventById("missing")
        );
        assertTrue(error.getMessage().contains("missing"));
    }

    @Test
    void updateEventRejectsCapacityBelowConfirmedRegistrations() {
        Event event = Event.builder()
                .id("evt-1")
                .sportId(1)
                .name("Torneo")
                .maxCapacity(10)
                .availableCapacity(2)
                .status(EventStatus.active)
                .build();
        EventUpdateRequest request = new EventUpdateRequest();
        request.setMaxCapacity(5);

        when(eventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent("evt-1", request)
        );
        assertTrue(error.getMessage().contains("inscripciones confirmadas"));
    }

    @Test
    void cancelAlreadyCancelledEventFails() {
        Event event = Event.builder()
                .id("evt-1")
                .sportId(1)
                .name("Torneo")
                .status(EventStatus.cancelled)
                .build();
        when(eventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> eventService.cancelEvent("evt-1")
        );
        assertEquals("El evento ya está cancelado.", error.getMessage());
    }

    @Test
    void calendarReturnsEmptyWhenRangeHasNoEvents() {
        stubStatusTransitions();
        LocalDate from = LocalDate.of(2099, 1, 1);
        LocalDate to = LocalDate.of(2099, 1, 31);
        when(eventRepository.findCalendarEvents(EventStatus.active, from, to)).thenReturn(List.of());

        List<CalendarEventResponse> calendar = eventService.getCalendar(from, to);

        assertTrue(calendar.isEmpty());
    }

    @Test
    void createEventSucceedsWithValidCapacity() {
        stubStatusTransitions();
        EventRequest request = new EventRequest();
        request.setSportId(1);
        request.setName("Torneo inclusivo");
        request.setMaxCapacity(20);
        request.setEventDate(LocalDate.now().plusDays(10));
        request.setEventTime(LocalTime.of(10, 0));

        Sport sport = Sport.builder().id(1).name("Fútbol Sala").build();
        Event saved = Event.builder()
                .id("evt-ok")
                .sportId(1)
                .name(request.getName())
                .maxCapacity(20)
                .availableCapacity(20)
                .status(EventStatus.draft)
                .build();

        when(sportRepository.findById(1)).thenReturn(Optional.of(sport));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(eventRepository.findById("evt-ok")).thenReturn(Optional.of(saved));

        var created = eventService.createEvent(request);
        assertEquals("evt-ok", created.getId());
        assertEquals(20, created.getMaxCapacity());
    }

    private void stubStatusTransitions() {
        when(eventRepository.findByStatus(EventStatus.draft)).thenReturn(Collections.emptyList());
        when(eventRepository.findByStatus(EventStatus.active)).thenReturn(Collections.emptyList());
        when(eventRepository.findByStatus(EventStatus.finished)).thenReturn(Collections.emptyList());
    }
}
