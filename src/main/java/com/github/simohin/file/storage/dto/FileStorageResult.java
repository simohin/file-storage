package com.github.simohin.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(description = "Result of file storage operation")
public class FileStorageResult {

    @Schema(description = "SHA-256 hash of stored file content",
            example = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3")
    private String hash;

    @Schema(description = "File size in bytes", example = "1048576")
    private long size;

    @Schema(description = "Detected MIME content type", example = "application/pdf")
    private String contentType;
}