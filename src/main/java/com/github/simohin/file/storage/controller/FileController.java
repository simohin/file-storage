package com.github.simohin.file.storage.controller;

import com.github.simohin.file.storage.common.FileConstants;
import com.github.simohin.file.storage.common.SortBy;
import com.github.simohin.file.storage.common.Visibility;
import com.github.simohin.file.storage.dto.ErrorResponse;
import com.github.simohin.file.storage.dto.FileListResponse;
import com.github.simohin.file.storage.dto.FileMetadataDto;
import com.github.simohin.file.storage.dto.FileRenameRequest;
import com.github.simohin.file.storage.dto.FileUploadResponse;
import com.github.simohin.file.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * REST Controller for file storage operations
 */
@RestController
@RequestMapping(FileConstants.API_FILES_PATH)
@Validated
@Tag(name = "File Storage", description = "API for file upload, download, management and listing")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "Upload a new file",
            description = "Upload a file with metadata including visibility settings and tags. " +
                    "The file content is detected automatically and a unique download URL is generated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or file already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "File size exceeds maximum limit",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during file processing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "The file to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "ID of the user uploading the file", required = true, example = "user123")
            @RequestParam @NotBlank String userId,

            @Parameter(description = "Name for the uploaded file", required = true, example = "document.pdf")
            @RequestParam @NotBlank String filename,

            @Parameter(description = "Visibility setting for the file", example = "PRIVATE")
            @RequestParam(defaultValue = "PRIVATE") Visibility visibility,

            @Parameter(description = "Comma-separated tags (max 5)", example = "work,important,document")
            @RequestParam(required = false) Set<String> tags
    ) {
        FileUploadResponse response = fileService.uploadFile(file, userId, filename, visibility, tags);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Download a file",
            description = "Download a file by its unique identifier. Private files can only be downloaded by their owner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - you don't have permission to download this file",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Unique file identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable String fileId,

            @Parameter(description = "User ID for access control", required = true, example = "user123")
            @RequestParam @NotBlank String userId
    ) {
        Resource resource = fileService.downloadFile(fileId, userId);
        return ResponseEntity.ok(resource);
    }

    @Operation(
            summary = "Delete a file",
            description = "Delete a file by its unique identifier. Only the file owner can delete their files."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "File deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - you can only delete your own files",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Unique file identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable String fileId,

            @Parameter(description = "User ID for access control", required = true, example = "user123")
            @RequestParam @NotBlank String userId
    ) {
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Rename a file",
            description = "Change the filename of an existing file. Only the file owner can rename their files."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File renamed successfully",
                    content = @Content(schema = @Schema(implementation = FileMetadataDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filename or filename already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - you can only rename your own files",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{fileId}/rename")
    public ResponseEntity<FileMetadataDto> renameFile(
            @Parameter(description = "Unique file identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable String fileId,

            @Parameter(description = "User ID for access control", required = true, example = "user123")
            @RequestParam @NotBlank String userId,

            @Valid @RequestBody FileRenameRequest request
    ) {
        FileMetadataDto updatedFile = fileService.renameFile(fileId, userId, request.getNewFilename());
        return ResponseEntity.ok(updatedFile);
    }

    @Operation(
            summary = "List all public files",
            description = "Get a paginated list of all public files with optional filtering by tags and sorting options."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Public files retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FileListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination or sorting parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/public")
    public ResponseEntity<FileListResponse> listPublicFiles(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (1-" + FileConstants.MAX_PAGE_SIZE + ")", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Filter by tags (case-insensitive)", example = "work,important")
            @RequestParam(required = false) Set<String> tags,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "UPLOAD_DATE") SortBy sortBy,

            @Parameter(description = "Sort in ascending order", example = "false")
            @RequestParam(defaultValue = "false") boolean ascending
    ) {
        FileListResponse response = fileService.listPublicFiles(page, size, tags, sortBy, ascending);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List user's files",
            description = "Get a paginated list of files belonging to a specific user with optional filtering and sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User files retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FileListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination or sorting parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<FileListResponse> listUserFiles(
            @Parameter(description = "User ID to list files for", required = true, example = "user123")
            @PathVariable @NotBlank String userId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (1-" + FileConstants.MAX_PAGE_SIZE + ")", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Filter by tags (case-insensitive)", example = "work,important")
            @RequestParam(required = false) Set<String> tags,

            @Parameter(description = "Filter by visibility", example = "PRIVATE")
            @RequestParam(required = false) Visibility visibility,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "UPLOAD_DATE") SortBy sortBy,

            @Parameter(description = "Sort in ascending order", example = "false")
            @RequestParam(defaultValue = "false") boolean ascending
    ) {
        FileListResponse response = fileService.listUserFiles(userId, page, size, tags, visibility, sortBy, ascending);
        return ResponseEntity.ok(response);
    }

}