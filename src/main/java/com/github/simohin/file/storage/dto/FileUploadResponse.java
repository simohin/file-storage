package com.github.simohin.file.storage.dto;

import com.github.simohin.file.storage.common.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for successful file upload
 */
@Data
@Schema(description = "Response containing uploaded file information")
public class FileUploadResponse {

    @Schema(description = "Unique file identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String fileId;

    @Schema(description = "Original filename", example = "document.pdf")
    private String filename;

    @Schema(description = "File visibility setting")
    private Visibility visibility;

    @Schema(description = "Detected or provided content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size in bytes", example = "1024000")
    private long size;

    @Schema(description = "Upload timestamp")
    private LocalDateTime uploadDate;

    @Size(max = 5, message = "Maximum 5 tags allowed")
    @Schema(description = "File tags (max 5)")
    private Set<String> tags;

    @Schema(description = "Unique download URL for the file",
            example = "http://localhost:8080/api/files/f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String downloadUrl;
}