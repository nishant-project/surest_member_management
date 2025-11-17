
package com.surest.surest_member_management.exception;

import com.surest.surest_member_management.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

import static com.surest.surest_member_management.constants.ApiConstants.INTERNAL_SERVER_ERROR;


@Slf4j
@ControllerAdvice

public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    // Handle validation errors (MethodArgumentNotValidException)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    } else {
                        return err.getObjectName() + ": " + err.getDefaultMessage();
                    }
                })
                .toList();

        ApiError apiError = buildApiError(
                String.valueOf(status.value()),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.join("; ", errors),
                request.getDescription(false).replace("uri=", "")
        );

        log.info("Validation failed at {}: {}", request.getDescription(false), errors);

        return ResponseEntity.status(status.value()).headers(headers).body(apiError);
    }

    // Handle forbidden access
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiError body = buildApiError(
                String.valueOf(HttpStatus.FORBIDDEN.value()),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Access is denied",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Handle resource not found
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        ApiError body = buildApiError(
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        ApiError body = buildApiError(
                String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Unauthorized access",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // Handle illegal arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError body = buildApiError(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument",
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // Catch-all for unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaught(HttpServletRequest request, Exception ex) {
        log.error("Unhandled exception at [{}]", request.getRequestURI(), ex);

        ApiError body = buildApiError(
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // Helper method to build ApiError
    private ApiError buildApiError(String status, String error, String message, String path) {
        int parsedStatus = 500;
        try {
            parsedStatus = Integer.parseInt(status);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse status '{}', defaulting to 500", status);
        }

        return ApiError.builder()
                .status(parsedStatus)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}
