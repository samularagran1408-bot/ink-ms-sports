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
    private String eventId;
    private String eventName;
    private LocalDateTime registrationDate;
    private Boolean attended;
    private Integer waitlistPosition;
    private String qrCode;
    private String message;
}