package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class EventResponse {
    private String id;
    private Integer sportId;
    private String sportName;
    private String name;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private Integer maxCapacity;
    private Integer availableCapacity;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
}