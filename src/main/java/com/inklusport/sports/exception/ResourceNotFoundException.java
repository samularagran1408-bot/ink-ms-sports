package com.inklusport.sports.exception;

/**
 * Recurso de dominio inexistente (discapacidad, evento, deporte, etc.).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
