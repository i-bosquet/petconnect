package com.petconnect.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the REST controllers.
 * Catches specific exceptions thrown by the application (services, controllers)
 * and maps them to appropriate HTTP responses with a consistent error format.
 *
 * @author ibosquet
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation exceptions triggered by @Valid on @RequestBody.
     * Returns HTTP 400 (Bad Request) with details about validation errors
     * in a standardized error body format.
     *
     * @param ex The MethodArgumentNotValidException caught.
     * @return A ResponseEntity containing the standardized error body and status 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
                Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles exceptions when an entity (e.g., Clinic, UserEntity) is not found.
     * Returns HTTP 404 (Not Found) with a standardized error body format.
     *
     * @param ex The EntityNotFoundException caught.
     * @return A ResponseEntity containing the standardized error body and status 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles exceptions related to data conflicts (e.g., duplicate email, username).
     * Returns HTTP 409 (Conflict) with a standardized error body format.
     *
     * @param ex The specific conflict exception caught (e.g., EmailAlreadyExistsException).
     * @return A ResponseEntity containing the standardized error body and status 409.
     */
    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            UsernameAlreadyExistsException.class,
            LicenseNumberAlreadyExistsException.class,
            VetPublicKeyAlreadyExistsException.class,
            MicrochipAlreadyExistsException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, Object>> handleConflictExceptions(RuntimeException ex) {
        log.warn("Conflict detected: {}", ex.getMessage());
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT, "Data Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handles Authentication exceptions (Bad Credentials, User Not Found during login).
     * Returns 401 Unauthorized with a standardized error body format.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class, AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        String message = "Invalid credentials or user could not be authenticated.";
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password provided.";
        } else if (ex instanceof UsernameNotFoundException) {
            message = "User account not found for the provided identifier.";
        }
        Map<String, Object> body = createErrorBody(HttpStatus.UNAUTHORIZED, "Authentication Failed", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Handles Authorization exceptions (Access Denied).
     * Returns 403 Forbidden with a standardized error body format.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action or access this resource.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }


    /**
     * Handles any other unhandled runtime exceptions (catch-all).
     * Returns HTTP 500 (Internal Server Error) with a standardized error body format.
     * Logs the full stack trace for debugging.
     *
     * @param ex The Exception caught.
     * @return A ResponseEntity containing the standardized error body and status 500.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        // *** USA EL HELPER y devuelve ResponseEntity ***
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Handles common business logic constraint violations like illegal state or arguments.
     * Returns 400 Bad Request or potentially 409 Conflict depending on the context.
     * Using 400 Bad Request generally for invalid operations based on current state.
     *
     * @param ex The RuntimeException caught (IllegalStateException or IllegalArgumentException).
     * @return A ResponseEntity containing the standardized error body and status 400.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadLogicExceptions(RuntimeException ex) {
        // Log as warning as it indicates a client potentially tried an invalid operation
        log.warn("Bad Request due to illegal state or argument: {}", ex.getMessage());
        // Decide if 400 or 409 is more appropriate. 400 is often used for invalid inputs/states.
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Creates a standardized error response body map.
     * Includes a timestamp, HTTP status code, a general error description,
     * and a detailed message (which can be a String or another structure like a Map for validation errors).
     *
     * @param status The HttpStatus code for the error.
     * @param error A short, general description of the error (e.g., "Validation Failed", "Resource Not Found").
     * @param message The detailed error message or structure containing specific error details.
     * @return A Map representing the standardized error response body.
     */
    private Map<String, Object> createErrorBody(HttpStatus status, String error, Object message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    /**
     * Handles exceptions caused by type mismatches in request parameters or path variables
     * (e.g., providing text when a number is expected, or an invalid enum value).
     * Returns HTTP 400 (Bad Request).
     *
     * @param ex The MethodArgumentTypeMismatchException caught.
     * @return A ResponseEntity containing the standardized error body and status 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Invalid value '%s' provided for parameter '%s'. Required type is '%s'.",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        log.warn("Type mismatch error: {}", error);
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Invalid Parameter Type", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles exceptions occurring when the request body is missing or cannot be parsed
     * (e.g., invalid JSON format) before validation occurs.
     * Returns HTTP 400 (Bad Request).
     *
     * @param ex The HttpMessageNotReadableException caught.
     * @return A ResponseEntity containing the standardized error body and status 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Failed to read request body: {}", ex.getMessage());
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Invalid Request Body", "Request body is missing or cannot be parsed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}