package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoutineRegistrationResponse {
    private String id;
    private String userId;
    private String routineId;
    private String routineName;
    private String trainerId;
    private String status;
    private String message;
    private LocalDateTime registrationDate;
}
