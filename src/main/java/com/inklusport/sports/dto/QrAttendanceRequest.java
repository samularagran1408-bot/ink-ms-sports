package com.inklusport.sports.dto;

import lombok.Data;

@Data
public class QrAttendanceRequest {
    private String qrCode;
    private String verifiedBy;
}
