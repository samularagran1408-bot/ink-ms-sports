package com.inklusport.sports.service;

import com.inklusport.sports.dto.AttendanceRequest;
import com.inklusport.sports.entity.EventAttendance;
import com.inklusport.sports.enums.CheckInMethod;
import com.inklusport.sports.repository.EventAttendanceRepository;
import com.inklusport.sports.repository.EventRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAttendanceService {

    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    @Transactional
    public String recordAttendance(AttendanceRequest request) {
        
        /**
         * Validar que la inscripción exista
         */
        boolean registrationExists = eventRegistrationRepository.existsById(request.getRegistrationId());
        if (!registrationExists) {
            throw new IllegalArgumentException("Error: La inscripción con ID '" + request.getRegistrationId() + "' no existe.");
        }

        /**
         * Validar que no se registró la asistencia previamente
         */
        if (eventAttendanceRepository.existsByRegistrationId(request.getRegistrationId())) {
            throw new IllegalStateException("Error: Ya se registró la asistencia para esta inscripción previamente.");
        }

        /**
         * Mapear y procesar el método de Check-In con el Enum de la Entidad
         */
        CheckInMethod method;
        try {
            /**
             * El método de check-in recibido puede ser "qr", "manual" o "admin".
             * En caso de no coincidir con ninguno de los valores permitidos, se usará el valor por defecto "qr".
             */
            method = CheckInMethod.valueOf(request.getCheckInMethod().toLowerCase().trim());
        } catch (Exception e) {
            log.warn("Método de check-in inválido recibido: {}. Se usará el valor por defecto.", request.getCheckInMethod());
            method = CheckInMethod.qr; // Respaldo por defecto
        }

        /**
         * Construcción limpia del objeto utilizando el patrón Builder de tu Entidad
         */
        EventAttendance attendance = EventAttendance.builder()
                .registrationId(request.getRegistrationId())
                .checkInMethod(method)
                .verifiedBy(request.getVerifiedBy())
                .build(); 
                
        /**
         * Guardar el objeto en la base de datos
         */
        EventAttendance saved = eventAttendanceRepository.save(attendance);
        log.info("Asistencia registrada exitosamente con ID: {}", saved.getId());

        return "Asistencia confirmada exitosamente. Código de registro: " + saved.getId();
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getAllAttendances() {
        log.info("Obteniendo listado global de asistencias");
        return eventAttendanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByEvent(String eventId) {
        log.info("Obteniendo asistencias para el evento con ID: {}", eventId);
        return eventAttendanceRepository.findByRegistration_EventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getAttendancesByRegistration(String registrationId) {
        log.info("Obteniendo asistencias para la inscripción con ID: {}", registrationId);
        return eventAttendanceRepository.findByRegistrationId(registrationId);
    }
}