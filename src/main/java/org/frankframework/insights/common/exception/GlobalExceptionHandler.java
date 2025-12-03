package org.frankframework.insights.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.authentication.ForbiddenException;
import org.frankframework.insights.authentication.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        log.error("Validation errors: {}", errors);

        ErrorResponse response =
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errors, HttpStatus.BAD_REQUEST.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        log.error(
                "Exception occurred: {} - {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);

        List<String> messages = exception.getMessage() == null ? List.of() : List.of(exception.getMessage());

        ErrorResponse response = new ErrorResponse(
                exception.getStatusCode().value(),
                messages,
                exception.getStatusCode().getReasonPhrase());

        return ResponseEntity.status(exception.getStatusCode()).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException exception, HttpServletRequest request) {
        log.warn(
                "401 Unauthorized: {} - Method: {} URL: {}",
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                List.of(exception.getMessage()),
                HttpStatus.UNAUTHORIZED.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception exception, HttpServletRequest request) {
        log.warn(
                "403 Forbidden: {} - Method: {} URL: {} - User: {}",
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");

        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                List.of(exception.getMessage() != null ? exception.getMessage() : "Access denied"),
                HttpStatus.FORBIDDEN.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        log.warn(
                "Authentication failed: {} - Method: {} URL: {}",
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                List.of("Authentication required"),
                HttpStatus.UNAUTHORIZED.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.warn(
                "Invalid argument: {} - Method: {} URL: {}",
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                List.of(exception.getMessage()),
                HttpStatus.BAD_REQUEST.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
