package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoutineResponse {
    private String id;
    private String trainerId;
    private Integer sportId;
    private String sportName;
    private String name;
    private String description;
    private String disabilityFocus;
    private String level;
    private Integer durationMinutes;
    private String exercisesJson;
    private String status;
    private Integer maxCapacity;
    private Integer availableCapacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
