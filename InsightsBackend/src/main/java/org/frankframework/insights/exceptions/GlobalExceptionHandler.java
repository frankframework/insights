package org.frankframework.insights.exceptions;

import java.util.List;
import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ErrorResponse response =
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errors, ErrorCode.BAD_REQUEST_ERROR.getCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getErrorCode().getStatus().value(),
                List.of(exception.getNote()),
                exception.getErrorCode().getCode());

        return ResponseEntity.status(exception.getErrorCode().getStatus()).body(response);
    }
}
