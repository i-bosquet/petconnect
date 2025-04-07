package com.petconnect.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the REST controllers.
 * Catches specific exceptions thrown by the application (services, controllers)
 * and maps them to appropriate HTTP responses with consistent error formats.
 *
 * @author ibosquet
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * Handles validation exceptions triggered by @Valid on @RequestBody.
     * Returns HTTP 400 (Bad Request) with details about validation errors.
     *
     * @param ex The MethodArgumentNotValidException caught.
     * @return A ResponseEntity containing a map of field errors and status 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Sets the HTTP status code
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        // Returning the map directly, @RestControllerAdvice handles JSON conversion
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles exceptions when an entity (e.g., Clinic, UserEntity) is not found.
     * Returns HTTP 404 (Not Found) with the exception message.
     *
     * @param ex The EntityNotFoundException caught.
     * @return A ResponseEntity containing the error message and status 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        Map<String, String> error = Map.of("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles exceptions related to duplicate email or username during registration.
     * Returns HTTP 409 (Conflict) with the specific error message.
     *
     * @param ex The EmailAlreadyExistsException or UsernameAlreadyExistsException caught.
     * @return A ResponseEntity containing the error message and status 409.
     */
    @ExceptionHandler({EmailAlreadyExistsException.class, UsernameAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict is appropriate for duplicate resources
    public ResponseEntity<Map<String, String>> handleConflictExceptions(RuntimeException ex) {
        // Logs specific exception type and message
        log.warn("Conflict during registration: {}", ex.getMessage());
        Map<String, String> error = Map.of("error", ex.getMessage()); // Use the message from the exception
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles Authentication exceptions (Bad Credentials, User Not Found during login).
     * Returns 401 Unauthorized.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class, AuthenticationException.class}) // Catch specific and base class
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        // Provide a more user-friendly message for common cases
        String message = "Invalid credentials or user not found.";
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password provided.";
        } else if (ex instanceof UsernameNotFoundException) {
            message = "UserEntity not found with the provided username."; // Or keep generic
        }
        Map<String, Object> body = createErrorBody(HttpStatus.UNAUTHORIZED, "Authentication Failed", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Handles Authorization exceptions (Access Denied).
     * Returns 403 Forbidden.
     * Note: Need to configure SecurityConfig to handle AccessDeniedException or use @PreAuthorize
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }


    /**
     * Handles any other unhandled runtime exceptions.
     * Returns HTTP 500 (Internal Server Error) with a generic message.
     * Logs the full stack trace for debugging.
     *
     * @param ex The Exception caught.
     * @return A ResponseEntity containing a generic error message and status 500.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Log the full exception for debugging purposes
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        Map<String, String> error = Map.of("error", "An internal server error occurred. Please try again later.");
        // Don't expose internal details (like stack trace) to the client in production
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // --- Helper Method for creating consistent error response body ---
    private Map<String, Object> createErrorBody(HttpStatus status, String error, Object message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message); // Can be String or Map (for validation errors)
        return body;
    }
}
