package com.github.simohin.file.storage.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * File visibility enumeration
 */
@Schema(description = "File visibility level")
public enum Visibility {
    @Schema(description = "File is private and only accessible by owner")
    PRIVATE,

    @Schema(description = "File is public and accessible by everyone")
    PUBLIC
}