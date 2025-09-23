package org.frankframework.webapp.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.frankframework.webapp.common.exception.ApiException;
import org.frankframework.webapp.common.exception.ErrorResponse;
import org.frankframework.webapp.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    public void handleValidationErrors_returnsBadRequestAndMessages() {
        FieldError error1 = new FieldError("object", "field1", "Field1 is invalid");
        FieldError error2 = new FieldError("object", "field2", "Field2 must not be null");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.httpStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.errorCode());
        assertEquals(List.of("Field1 is invalid", "Field2 must not be null"), body.messages());
    }

    @Test
    public void handleValidationErrors_emptyErrorList_returnsBadRequestWithEmptyMessages() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().messages().isEmpty());
    }

    @Test
    public void handleApiException_returnsCorrectStatusAndMessage() {
        String msg = "Custom API error";
        ApiException ex = new ApiException(msg, HttpStatus.NOT_FOUND, null);

        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.httpStatus());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), body.errorCode());
        assertEquals(List.of(msg), body.messages());
    }

    @Test
    public void handleApiException_handlesNullMessage() {
        ApiException ex = new ApiException(null, HttpStatus.BAD_GATEWAY, null);

        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_GATEWAY.value(), body.httpStatus());
        assertEquals(HttpStatus.BAD_GATEWAY.getReasonPhrase(), body.errorCode());
        assertTrue(body.messages().isEmpty());
    }
}
