package com.github.simohin.file.storage.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * File sorting options
 */
@Schema(description = "Available sorting criteria for file listings")
public enum SortBy {
    @Schema(description = "Sort by file name")
    FILENAME,

    @Schema(description = "Sort by upload date")
    UPLOAD_DATE,

    @Schema(description = "Sort by file tags")
    TAG,

    @Schema(description = "Sort by content type")
    CONTENT_TYPE,

    @Schema(description = "Sort by file size")
    FILE_SIZE
}