package com.github.simohin.file.storage.dto;

import com.github.simohin.file.storage.common.FileStatus;
import com.github.simohin.file.storage.common.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Accessors(chain = true)
@Schema(description = "File metadata information")
public class FileMetadataDto {

    @Schema(description = "Unique file identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @NotBlank(message = "File name is required")
    @Schema(description = "Original file name", example = "document.pdf")
    private String fileName;

    @NotBlank(message = "User ID is required")
    @Schema(description = "User identifier who uploaded the file", example = "user123")
    private String userId;

    @NotNull(message = "Visibility is required")
    @Schema(description = "File visibility setting")
    private Visibility visibility;

    @Size(max = 5, message = "Maximum 5 tags allowed")
    @Schema(description = "File tags for categorization (max 5)", example = "[\"document\", \"important\"]")
    private Set<String> tags;

    @Schema(description = "File upload timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime uploadDate;

    @Schema(description = "MIME content type", example = "application/pdf")
    private String contentType;

    @Positive(message = "File size must be positive")
    @Schema(description = "File size in bytes", example = "1048576")
    private long size;

    @Schema(description = "SHA-256 hash of file content", example = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3")
    private String hash;

    @NotNull(message = "File status is required")
    @Schema(description = "Current file status")
    private FileStatus status;


    @Schema(description = "Human-readable file size", example = "1.5 MB")
    public String getFormattedSize() {
        if (size == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", fileSize, units[unitIndex]);
    }
}