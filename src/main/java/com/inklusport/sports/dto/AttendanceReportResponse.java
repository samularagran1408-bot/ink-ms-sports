package com.inklusport.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportResponse {
    private String eventId;
    private String eventName;
    private long totalRegistered;
    private long totalAttended;
    private long totalAbsent;
    private double attendanceRatePercent;
    private List<AttendeeRow> attendees;
    private List<AbsentRow> absentees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendeeRow {
        private String registrationId;
        private String userId;
        private String fullName;
        private String email;
        private LocalDateTime checkInTime;
        private String checkInMethod;
        private String verifiedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsentRow {
        private String registrationId;
        private String userId;
        private String fullName;
        private String email;
    }
}
