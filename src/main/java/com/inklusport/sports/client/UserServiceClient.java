package com.inklusport.sports.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ink-ms-users", url = "${users.service.url:http://localhost:3002}", fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") String id);

    @GetMapping("/api/internal/users/{id}")
    Map<String, Object> getUserByIdInternal(@PathVariable("id") String id);

    @GetMapping("/api/users/verify/status/{userId}")
    Map<String, Object> getVerificationStatus(@PathVariable("userId") String userId);

    @GetMapping("/api/admin/users/roles-by-email")
    List<String> getUserRoles(@RequestParam("email") String email);

    @GetMapping("/api/internal/users/id-by-email")
    Map<String, String> getUserIdByEmail(@RequestParam("email") String email);

    @GetMapping("/api/internal/users/emails-by-role")
    List<String> getEmailsByRole(@RequestParam("role") String role);
}