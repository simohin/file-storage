package com.github.simohin.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Standard error response DTO
 */
@Data
@Schema(description = "Standard error response format")
public class ErrorResponse {

    @Schema(description = "Error code identifier", example = "FILE_NOT_FOUND")
    private String code;

    @Schema(description = "Human-readable error message",
            example = "File with ID 'f47ac10b-58cc-4372-a567-0e02b2c3d479' not found")
    private String message;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Timestamp when error occurred")
    private Instant timestamp;

    @Schema(description = "Request path where error occurred", example = "/api/files/f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String path;

    @Schema(description = "Additional error details (optional)", example = "File may have been deleted or you don't have access")
    private String details;

    public ErrorResponse(String code, String message, int status, String path) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String code, String message, int status, String path, String details) {
        this(code, message, status, path);
        this.details = details;
    }
}