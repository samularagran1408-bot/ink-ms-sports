package com.inklusport.sports.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId;
    private String type;
    private String title;
    private String body;
    private String eventId;
    private String priority;
}