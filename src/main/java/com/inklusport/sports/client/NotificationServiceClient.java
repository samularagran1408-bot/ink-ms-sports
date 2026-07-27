package com.inklusport.sports.client;

import com.inklusport.sports.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "accessibility-ms", url = "${accessibility.service.url:http://localhost:3004}")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/internal/create")
    void createNotification(@RequestHeader("X-User-Id") String userId,
                            @RequestBody NotificationRequest request);
}