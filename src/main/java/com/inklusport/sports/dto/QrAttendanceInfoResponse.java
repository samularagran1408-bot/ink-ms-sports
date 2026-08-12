package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrAttendanceInfoResponse {
    private String qrCode;
    private String registrationId;
    private String eventId;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String location;
    private Boolean attended;
    private Boolean ownedByCurrentUser;
}
