package com.github.simohin.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for file listing with pagination information
 */
@Data
@Schema(description = "Paginated response containing list of files")
public class FileListResponse {

    @Schema(description = "List of files in current page")
    private List<FileMetadataDto> files;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of elements", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    @Schema(description = "Whether there are more pages", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there are previous pages", example = "false")
    private boolean hasPrevious;
}