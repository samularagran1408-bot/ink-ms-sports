package com.inklusport.sports.service;

import com.inklusport.sports.client.NotificationServiceClient;
import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notificaciones orientadas a staff (organizador, entrenador, admin).
 * El userId de notificación es el email (mismo principal JWT).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffNotificationService {

    private final NotificationServiceClient notificationClient;
    private final UserServiceClient userServiceClient;

    @Value("${notifications.admin-emails:}")
    private String adminEmails;

    public void notifyUser(String userIdOrEmail, String type, String title, String body, String eventId) {
        String recipient = resolveEmail(userIdOrEmail);
        if (recipient == null) {
            log.warn("No se pudo resolver email para notificar a {}", userIdOrEmail);
            return;
        }
        send(recipient, type, title, body, eventId, "high");
    }

    public void notifyOrganizer(String organizerIdOrEmail, String type, String title, String body, String eventId) {
        notifyUser(organizerIdOrEmail, type, title, body, eventId);
    }

    public void notifyTrainer(String trainerIdOrEmail, String type, String title, String body, String eventId) {
        notifyUser(trainerIdOrEmail, type, title, body, eventId);
    }

    public void notifyAdmins(String type, String title, String body, String eventId) {
        if (adminEmails == null || adminEmails.isBlank()) {
            return;
        }
        Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(email -> email.contains("@"))
                .collect(Collectors.toSet())
                .forEach(email -> send(email, type, title, body, eventId, "medium"));
    }

    private void send(String email, String type, String title, String body, String eventId, String priority) {
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(email);
            request.setType(type);
            request.setTitle(title);
            request.setBody(body);
            request.setEventId(eventId);
            request.setPriority(priority);
            notificationClient.createNotification(email, request);
            log.info("Notificación staff enviada a {} [{}]", email, type);
        } catch (Exception e) {
            log.error("Error notificando a {}: {}", email, e.getMessage());
        }
    }

    private String resolveEmail(String userIdOrEmail) {
        if (userIdOrEmail == null || userIdOrEmail.isBlank()) {
            return null;
        }
        String value = userIdOrEmail.trim();
        if (value.contains("@")) {
            return value;
        }
        try {
            Map<String, Object> user = userServiceClient.getUserByIdInternal(value);
            if (user != null && user.get("email") != null) {
                String email = String.valueOf(user.get("email"));
                if (email.contains("@") && !email.startsWith("no-disponible")) {
                    return email;
                }
            }
        } catch (Exception e) {
            log.debug("No se resolvió email para {}: {}", value, e.getMessage());
        }
        try {
            Map<String, Object> user = userServiceClient.getUserById(value);
            if (user != null && user.get("email") != null) {
                String email = String.valueOf(user.get("email"));
                if (email.contains("@") && !email.startsWith("no-disponible")) {
                    return email;
                }
            }
        } catch (Exception e) {
            log.debug("Fallback getUserById falló para {}: {}", value, e.getMessage());
        }
        return null;
    }
}
