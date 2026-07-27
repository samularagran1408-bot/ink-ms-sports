package com.inklusport.sports.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistResponse {
    private String id;
    private String userId;
    private String eventId;
    private LocalDateTime requestedAt;
    private Integer position;
    private String status;
}