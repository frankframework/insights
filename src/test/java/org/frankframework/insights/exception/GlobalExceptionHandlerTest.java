package org.frankframework.insights.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.frankframework.insights.authentication.ForbiddenException;
import org.frankframework.insights.authentication.UnauthorizedException;
import org.frankframework.insights.common.exception.ApiException;
import org.frankframework.insights.common.exception.ErrorResponse;
import org.frankframework.insights.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
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

    @Test
    public void handleUnauthorized_returnsUnauthorizedWithMessage() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/secure-endpoint");

        String errorMessage = "User not authenticated";
        UnauthorizedException ex = new UnauthorizedException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.httpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.errorCode());
        assertEquals(List.of(errorMessage), body.messages());
    }

    @Test
    public void handleUnauthorized_logsRequestDetails() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/business-value");

        UnauthorizedException ex = new UnauthorizedException("Authentication required");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertNotNull(response);
        verify(request).getMethod();
        verify(request).getRequestURI();
    }

    @Test
    public void handleForbidden_withForbiddenException_returnsForbiddenWithMessage() {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/admin/users");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(request.getUserPrincipal()).thenReturn(principal);

        String errorMessage = "Insufficient permissions";
        ForbiddenException ex = new ForbiddenException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.httpStatus());
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.errorCode());
        assertEquals(List.of(errorMessage), body.messages());
    }

    @Test
    public void handleForbidden_withAccessDeniedException_returnsForbiddenWithMessage() {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/protected");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(request.getUserPrincipal()).thenReturn(principal);

        String errorMessage = "Access denied to resource";
        AccessDeniedException ex = new AccessDeniedException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.httpStatus());
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.errorCode());
        assertEquals(List.of(errorMessage), body.messages());
    }

    @Test
    public void handleForbidden_withNullMessage_returnsDefaultMessage() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/resource");
        when(request.getUserPrincipal()).thenReturn(null);

        AccessDeniedException ex = new AccessDeniedException(null);

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(List.of("Access denied"), body.messages());
    }

    @Test
    public void handleForbidden_withAnonymousUser_logsAnonymous() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/resource");
        when(request.getUserPrincipal()).thenReturn(null);

        ForbiddenException ex = new ForbiddenException("Not authorized");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertNotNull(response);
        verify(request).getUserPrincipal();
    }

    @Test
    public void handleAuthenticationException_returnsUnauthorizedWithGenericMessage() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/login");

        AuthenticationException ex = new AuthenticationException("Bad credentials") {};

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.httpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.errorCode());
        assertEquals(List.of("Authentication required"), body.messages());
    }

    @Test
    public void handleAuthenticationException_logsRequestDetails() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/profile");

        AuthenticationException ex = new AuthenticationException("Invalid token") {};

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertNotNull(response);
        verify(request).getMethod();
        verify(request).getRequestURI();
    }

    @Test
    public void handleAuthenticationException_withNullMessage_stillReturnsGenericMessage() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/data");

        AuthenticationException ex = new AuthenticationException(null) {};

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(List.of("Authentication required"), body.messages());
    }

    @Test
    public void handleApiException_withDifferentHttpStatuses() {
        HttpStatus[] statuses = {
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.CONFLICT,
            HttpStatus.UNPROCESSABLE_ENTITY
        };

        for (HttpStatus status : statuses) {
            ApiException ex = new ApiException("Error for " + status, status, null);
            ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

            assertEquals(status, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(status.value(), response.getBody().httpStatus());
        }
    }

    @Test
    public void handleValidationErrors_withMultipleFieldErrors_returnsAllMessages() {
        FieldError error1 = new FieldError("user", "email", "Email is required");
        FieldError error2 = new FieldError("user", "password", "Password too short");
        FieldError error3 = new FieldError("user", "username", "Username already exists");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2, error3));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.messages().size());
        assertTrue(body.messages().contains("Email is required"));
        assertTrue(body.messages().contains("Password too short"));
        assertTrue(body.messages().contains("Username already exists"));
    }
}
