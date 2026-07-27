package com.inklusport.sports.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private String id;
    private String registrationId;
    private LocalDateTime checkInTime;
    private String checkInMethod;
    private String verifiedBy;
}