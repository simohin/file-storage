package com.github.simohin.file.storage.controller;

import com.github.simohin.file.storage.dto.ErrorResponse;
import com.github.simohin.file.storage.service.DiskSpaceService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers
 */
@RestControllerAdvice
@Slf4j
@Hidden // Hide from Swagger documentation
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed: " + message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                "CONSTRAINT_VIOLATION",
                "Constraint violation: " + message,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        log.warn("Constraint violation: {}", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "FILE_TOO_LARGE",
                "File size exceeds maximum allowed limit",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                request.getRequestURI(),
                "Maximum file size allowed: " + ex.getMaxUploadSize() + " bytes"
        );

        log.warn("File upload size exceeded: {}", ex.getMaxUploadSize());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(
            UnsupportedOperationException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "NOT_IMPLEMENTED",
                "This feature is not yet implemented: " + ex.getMessage(),
                HttpStatus.NOT_IMPLEMENTED.value(),
                request.getRequestURI()
        );

        log.info("Not implemented operation called: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                "Invalid argument: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DiskSpaceService.DiskSpaceException.class)
    public ResponseEntity<ErrorResponse> handleDiskSpaceException(
            DiskSpaceService.DiskSpaceException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "DISK_SPACE_EXCEEDED",
                ex.getMessage(),
                HttpStatus.INSUFFICIENT_STORAGE.value(),
                request.getRequestURI(),
                "Storage limit of 200MB exceeded. Please delete some files or contact administrator."
        );

        log.warn("Disk space limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                "Please contact support if this problem persists"
        );

        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}