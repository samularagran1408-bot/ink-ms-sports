package com.inklusport.sports.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Actúa como un interceptor/ try-catch global para toda la app
 */
@RestControllerAdvice 
public class GlobalExceptionHandler {

    /**
     * Captura errores de lógica de negocio (Ej: "El usuario ya está registrado")
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "ERROR", ex.getMessage());
    }

    /**
     * Captura argumentos inválidos (Ej: "Evento no encontrado")
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "ERROR", ex.getMessage());
    }

    /**
     * El "catch (Exception e)" definitivo: Captura cualquier error inesperado del sistema
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        // En producción podrías usar log.error("Fatal error: ", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "FATAL_ERROR", 
                "Ha ocurrido un error interno inesperado: " + ex.getMessage()
        );
    }

    /**
     * Método auxiliar para mantener las respuestas estandarizadas y limpias
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String errorType, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", errorType);
        body.put("code", status.value());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}