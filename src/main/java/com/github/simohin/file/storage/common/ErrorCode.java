package com.github.simohin.file.storage.common;

public enum ErrorCode {
    FILE_EMPTY("File cannot be empty"),
    USER_ID_EMPTY("User ID cannot be empty"),
    FILENAME_EMPTY("Filename cannot be empty"),
    TOO_MANY_TAGS("Maximum %d tags allowed, but %d provided"),
    INVALID_FILE_ID("Invalid file ID format: %s"),
    FILE_NOT_FOUND("File not found: %s"),
    ACCESS_DENIED("Access denied to file: %s"),
    FILE_CONTENT_NOT_FOUND("File content not found: %s"),
    FILENAME_EXISTS("File with name '%s' already exists for user"),
    CONTENT_EXISTS("File with identical content already exists for user: %s"),
    OWNER_ONLY_DELETE("Access denied: Only the file owner can delete this file"),
    OWNER_ONLY_RENAME("Access denied: Only the file owner can rename this file"),
    NEW_FILENAME_EMPTY("New filename cannot be empty"),
    NEW_FILENAME_EXISTS("File with name '%s' already exists for user");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}