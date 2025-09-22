package com.github.simohin.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for renaming a file
 */
@Data
@Schema(description = "Request to rename an existing file")
public class FileRenameRequest {

    @NotBlank(message = "New filename is required")
    @Schema(description = "New filename for the file", example = "renamed_document.pdf", required = true)
    private String newFilename;
}