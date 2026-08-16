package com.inklusport.sports.dto;

import lombok.*;
import java.time.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private String id;
    private String userId;
    private String userFullName;
    private String userEmail;
    private String eventId;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String eventStatus;
    private LocalDateTime registrationDate;
    private Boolean attended;
    private Integer waitlistPosition;
    private String qrCode;
    private String message;
}