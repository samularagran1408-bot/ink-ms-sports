package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class CalendarEventResponse {
    private String id;
    private String title;
    private LocalDate startDate;
    private LocalTime startTime;
    private String location;
    private String sportName;
    private Integer availableCapacity;
    private Integer maxCapacity;
}