package com.github.simohin.file.storage.util;

import com.github.simohin.file.storage.common.ErrorCode;
import com.github.simohin.file.storage.common.FileConstants;

import java.util.UUID;

public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Validates and parses file ID string to UUID
     *
     * @param fileId String representation of file ID
     * @return UUID object
     * @throws IllegalArgumentException if fileId format is invalid
     */
    public static UUID validateAndParseFileId(String fileId) {
        try {
            return UUID.fromString(fileId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ErrorCode.INVALID_FILE_ID.format(fileId));
        }
    }

    /**
     * Validates that string is not null or empty/whitespace
     *
     * @param value        String to validate
     * @param errorMessage Error message if validation fails
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void validateNotEmpty(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Validates page parameters for pagination
     *
     * @param page Page number (must be >= 0)
     * @param size Page size (must be > 0 and <= MAX_PAGE_SIZE)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        if (size > FileConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + FileConstants.MAX_PAGE_SIZE);
        }
    }
}