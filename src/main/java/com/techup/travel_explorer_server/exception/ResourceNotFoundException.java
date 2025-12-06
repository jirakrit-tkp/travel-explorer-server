package com.techup.travel_explorer_server.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s with id %d not found", resource, id));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

