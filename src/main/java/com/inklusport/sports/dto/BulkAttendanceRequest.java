package com.inklusport.sports.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BulkAttendanceRequest {
    private List<String> registrationIds = new ArrayList<>();
    private String checkInMethod;
    private String verifiedBy;
}
