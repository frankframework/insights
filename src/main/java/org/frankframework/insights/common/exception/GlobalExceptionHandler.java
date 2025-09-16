package org.frankframework.insights.common.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
