package com.aiproof.studycompanion.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.aiproof.studycompanion.ai.AiException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Every uncaught error becomes one readable JSON response.
 *
 * The important rule here: an AI provider failure NEVER comes back as 403.
 * A 403 from Groq means "Groq rejected us", not "the student is not logged in",
 * so it is reported as 502/503 with a safe message. That is what stopped the
 * old confusing "Access denied (403)" message in the browser console.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------- AI provider problems ---------- */
    @ExceptionHandler(AiException.class)
    public ResponseEntity<Map<String, Object>> handleAi(
            AiException ex, HttpServletRequest request) {

        // Full detail goes to the server log only.
        log.error("AI_ERROR | provider={} | providerStatus={} | {}",
                ex.getProviderName(), ex.getHttpStatus(), ex.getMessage());

        HttpStatus status = ex.getHttpStatus() == 429
                ? HttpStatus.TOO_MANY_REQUESTS
                : (ex.isRetryable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY);

        // Only the safe message goes to the browser. Never the key, never a stack trace.
        return build(status, "AI_SERVICE_ERROR", ex.getUserMessage(), request);
    }

    /* ---------- real ownership / permission denial ---------- */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("ACCESS_DENIED | {} {} | {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                ex.getMessage() == null ? "You do not own this resource." : ex.getMessage(),
                request);
    }

    /* ---------- empty material, bad input, unparseable quiz ---------- */
    @ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    public ResponseEntity<Map<String, Object>> handleBadState(
            RuntimeException ex, HttpServletRequest request) {

        log.warn("BAD_REQUEST | {} {} | {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    /* ---------- @Valid failures ---------- */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        StringBuilder message = new StringBuilder("Invalid request: ");
        ex.getBindingResult().getFieldErrors().forEach(field ->
                message.append(field.getField()).append(" ")
                       .append(field.getDefaultMessage()).append("; "));

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message.toString(), request);
    }

    /* ---------- thrown on purpose with a status ---------- */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        return build(status, status.name(), ex.getReason(), request);
    }

    /* ---------- "Material not found" / "Project not found" etc. ---------- */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {

        String message = ex.getMessage() == null ? "" : ex.getMessage();

        // MaterialService, ProjectService and SpaceService throw plain
        // RuntimeExceptions with messages like "Material not found".
        // Those are genuinely 404s, not server crashes.
        if (message.toLowerCase().contains("not found")) {
            log.warn("NOT_FOUND | {} {} | {}",
                    request.getMethod(), request.getRequestURI(), message);
            return build(HttpStatus.NOT_FOUND, "NOT_FOUND", message, request);
        }

        log.error("INTERNAL_ERROR | {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong on the server. Please try again.", request);
    }

    /* ---------- last resort ---------- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleEverythingElse(
            Exception ex, HttpServletRequest request) {

        log.error("INTERNAL_ERROR | {} {}", request.getMethod(), request.getRequestURI(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong on the server. Please try again.", request);
    }

    /* ---------- helper ---------- */

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message == null ? "No message available." : message);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
