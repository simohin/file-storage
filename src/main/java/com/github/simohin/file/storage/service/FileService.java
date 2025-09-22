package com.github.simohin.file.storage.service;

import com.github.simohin.file.storage.common.ErrorCode;
import com.github.simohin.file.storage.common.FileConstants;
import com.github.simohin.file.storage.common.FileStatus;
import com.github.simohin.file.storage.common.SortBy;
import com.github.simohin.file.storage.common.Visibility;
import com.github.simohin.file.storage.dto.FileListResponse;
import com.github.simohin.file.storage.dto.FileMetadataDto;
import com.github.simohin.file.storage.dto.FileStorageResult;
import com.github.simohin.file.storage.dto.FileUploadResponse;
import com.github.simohin.file.storage.entity.FileMetadata;
import com.github.simohin.file.storage.mapper.FileMetadataMapper;
import com.github.simohin.file.storage.repository.FileMetadataRepository;
import com.github.simohin.file.storage.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorageService fileStorageService;
    private final FileMetadataService fileMetadataService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;
    private final DiskSpaceService diskSpaceService;

    public FileUploadResponse uploadFile(MultipartFile file, String userId, String filename,
                                         Visibility visibility, Set<String> tags) {
        validateUploadParameters(file, userId, filename, tags);

        try {
            diskSpaceService.validateDiskSpace(file.getSize());
            checkForDuplicates(userId, filename, file);

            UUID fileId = UUID.randomUUID();
            FileStorageResult storageResult = storeFile(fileId, file, filename);
            FileMetadataDto metadata = createFileMetadata(fileId, filename, userId, storageResult, visibility, tags);

            return buildUploadResponse(fileId, filename, visibility, storageResult, metadata, tags);

        } catch (Exception e) {
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    private FileStorageResult storeFile(UUID fileId, MultipartFile file, String filename) {
        try {
            return fileStorageService.saveFile(fileId, file.getInputStream(), filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process file: " + filename, e);
        }
    }

    private FileMetadataDto createFileMetadata(UUID fileId, String filename, String userId,
                                               FileStorageResult storageResult, Visibility visibility, Set<String> tags) {
        FileMetadataDto metadata = fileMetadataService.createFileMetadata(
                fileId, filename, userId, storageResult.getContentType(),
                storageResult.getSize(), storageResult.getHash(), visibility,
                tags != null ? tags : Set.of()
        );

        fileMetadataService.updateFileStatus(fileId, FileStatus.ACTIVE);
        return metadata;
    }

    private FileUploadResponse buildUploadResponse(UUID fileId, String filename, Visibility visibility,
                                                   FileStorageResult storageResult, FileMetadataDto metadata, Set<String> tags) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileId.toString());
        response.setFilename(filename);
        response.setVisibility(visibility);
        response.setContentType(storageResult.getContentType());
        response.setSize(storageResult.getSize());
        response.setUploadDate(metadata.getUploadDate());
        response.setTags(tags != null ? tags : Set.of());
        response.setDownloadUrl(FileConstants.API_FILES_PATH + "/" + fileId);
        return response;
    }

    public Resource downloadFile(String fileId, String userId) {
        UUID uuid = ValidationUtils.validateAndParseFileId(fileId);

        Optional<FileMetadataDto> metadataOpt = fileMetadataService.getFileMetadata(uuid);
        if (metadataOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.format(fileId));
        }

        FileMetadataDto metadata = metadataOpt.get();

        boolean hasAccess = metadata.getVisibility() == Visibility.PUBLIC ||
                metadata.getUserId().equals(userId);

        if (!hasAccess) {
            throw new RuntimeException(ErrorCode.ACCESS_DENIED.format(fileId));
        }

        Optional<InputStream> fileStreamOpt = fileStorageService.getFile(uuid);
        if (fileStreamOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.FILE_CONTENT_NOT_FOUND.format(fileId));
        }

        try {
            return new org.springframework.core.io.InputStreamResource(fileStreamOpt.get()) {
                @Override
                public String getFilename() {
                    return metadata.getFileName();
                }

                @Override
                public long contentLength() {
                    return metadata.getSize();
                }
            };
        } catch (Exception e) {
            log.error("Failed to create resource for file download: {}", fileId, e);
            throw new RuntimeException("Failed to prepare file for download: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileId, String userId) {
        UUID uuid = ValidationUtils.validateAndParseFileId(fileId);

        Optional<FileMetadataDto> metadataOpt = fileMetadataService.getFileMetadata(uuid);
        if (metadataOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.format(fileId));
        }

        FileMetadataDto metadata = metadataOpt.get();

        if (!metadata.getUserId().equals(userId)) {
            throw new RuntimeException(ErrorCode.OWNER_ONLY_DELETE.getMessage());
        }

        try {
            boolean storageDeleted = fileStorageService.deleteFile(uuid);
            if (!storageDeleted) {
                log.warn("File was not found in storage but metadata exists: {}", fileId);
            }

            boolean metadataDeleted = fileMetadataService.deleteFileMetadata(uuid);
            if (!metadataDeleted) {
                throw new RuntimeException("Failed to mark file metadata as deleted");
            }


        } catch (Exception e) {
            log.error("File deletion failed for fileId: {}, userId: {}", fileId, userId, e);
            throw new RuntimeException("File deletion failed: " + e.getMessage(), e);
        }
    }

    public FileMetadataDto renameFile(String fileId, String userId, String newFilename) {
        UUID uuid = ValidationUtils.validateAndParseFileId(fileId);
        ValidationUtils.validateNotEmpty(newFilename, ErrorCode.NEW_FILENAME_EMPTY.getMessage());

        FileMetadataDto metadata = getFileMetadataForRename(uuid, fileId);
        validateRenamePermissions(metadata, userId);
        validateNewFilenameAvailable(userId, newFilename.trim(), metadata.getFileName());

        return updateFileMetadata(uuid, newFilename.trim(), fileId, userId);
    }

    private FileMetadataDto getFileMetadataForRename(UUID uuid, String fileId) {
        Optional<FileMetadataDto> metadataOpt = fileMetadataService.getFileMetadata(uuid);
        if (metadataOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.FILE_NOT_FOUND.format(fileId));
        }
        return metadataOpt.get();
    }

    private void validateRenamePermissions(FileMetadataDto metadata, String userId) {
        if (!metadata.getUserId().equals(userId)) {
            throw new RuntimeException(ErrorCode.OWNER_ONLY_RENAME.getMessage());
        }
    }

    private void validateNewFilenameAvailable(String userId, String newFilename, String currentFilename) {
        if (!currentFilename.equals(newFilename)) {
            Optional<FileMetadataDto> existingFile = fileMetadataService.checkFileExists(userId, newFilename);
            if (existingFile.isPresent()) {
                throw new IllegalArgumentException(ErrorCode.NEW_FILENAME_EXISTS.format(newFilename));
            }
        }
    }

    private FileMetadataDto updateFileMetadata(UUID uuid, String newFilename, String fileId, String userId) {
        try {
            FileMetadataDto updateDto = new FileMetadataDto();
            updateDto.setFileName(newFilename);

            Optional<FileMetadataDto> updatedMetadataOpt = fileMetadataService.updateFileMetadata(uuid, updateDto);
            if (updatedMetadataOpt.isEmpty()) {
                throw new RuntimeException("Failed to update file metadata");
            }

            return updatedMetadataOpt.get();

        } catch (Exception e) {
            log.error("File rename failed for fileId: {}, userId: {}, newFilename: {}", fileId, userId, newFilename, e);
            throw new RuntimeException("File rename failed: " + e.getMessage(), e);
        }
    }

    public FileListResponse listPublicFiles(int page, int size, Set<String> tags,
                                            SortBy sortBy, boolean ascending) {
        validatePaginationParameters(page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, ascending);
            Page<FileMetadata> resultPage;

            if (tags != null && !tags.isEmpty()) {
                resultPage = fileMetadataRepository.findPublicActiveFilesByTags(tags, pageable);
            } else {
                resultPage = fileMetadataRepository.findPublicActiveFiles(pageable);
            }

            List<FileMetadataDto> files = resultPage.getContent()
                    .stream()
                    .map(fileMetadataMapper::toDto)
                    .collect(Collectors.toList());

            return createFileListResponse(files, resultPage);

        } catch (Exception e) {
            log.error("Failed to list public files - page: {}, size: {}, tags: {}", page, size, tags, e);
            throw new RuntimeException("Failed to list public files: " + e.getMessage(), e);
        }
    }

    public FileListResponse listUserFiles(String userId, int page, int size, Set<String> tags,
                                          Visibility visibility, SortBy sortBy, boolean ascending) {
        validatePaginationParameters(page, size);
        ValidationUtils.validateNotEmpty(userId, ErrorCode.USER_ID_EMPTY.getMessage());

        try {
            Pageable pageable = createPageable(page, size, sortBy, ascending);
            Page<FileMetadata> resultPage;

            if (tags != null && !tags.isEmpty() && visibility != null) {
                // Both tags and visibility filter
                resultPage = fileMetadataRepository.findByUserIdAndStatusAndVisibilityAndTagsIn(
                        userId, FileStatus.ACTIVE, visibility, tags, pageable);
            } else if (tags != null && !tags.isEmpty()) {
                // Only tags filter
                resultPage = fileMetadataRepository.findByUserIdAndStatusAndTagsIn(
                        userId, FileStatus.ACTIVE, tags, pageable);
            } else if (visibility != null) {
                // Only visibility filter
                resultPage = fileMetadataRepository.findByUserIdAndStatusAndVisibility(
                        userId, FileStatus.ACTIVE, visibility, pageable);
            } else {
                // No filters, all user files
                resultPage = fileMetadataRepository.findByUserIdAndStatus(
                        userId, FileStatus.ACTIVE, pageable);
            }

            List<FileMetadataDto> files = resultPage.getContent()
                    .stream()
                    .map(fileMetadataMapper::toDto)
                    .collect(Collectors.toList());

            return createFileListResponse(files, resultPage);

        } catch (Exception e) {
            log.error("Failed to list user files for userId: {} - page: {}, size: {}, tags: {}, visibility: {}",
                    userId, page, size, tags, visibility, e);
            throw new RuntimeException("Failed to list user files: " + e.getMessage(), e);
        }
    }

    /**
     * Validates upload parameters according to requirements
     */
    private void validateUploadParameters(MultipartFile file, String userId, String filename, Set<String> tags) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.FILE_EMPTY.getMessage());
        }

        ValidationUtils.validateNotEmpty(userId, ErrorCode.USER_ID_EMPTY.getMessage());
        ValidationUtils.validateNotEmpty(filename, ErrorCode.FILENAME_EMPTY.getMessage());

        // Validate tags - maximum allowed
        if (tags != null && tags.size() > FileConstants.MAX_TAGS_ALLOWED) {
            throw new IllegalArgumentException(ErrorCode.TOO_MANY_TAGS.format(FileConstants.MAX_TAGS_ALLOWED, tags.size()));
        }

    }

    /**
     * Checks for duplicate files based on filename or content hash for the user
     */
    private void checkForDuplicates(String userId, String filename, MultipartFile file) throws IOException {
        // Check for filename duplicate
        Optional<FileMetadataDto> existingByFilename = fileMetadataService.checkFileExists(userId, filename);
        if (existingByFilename.isPresent()) {
            throw new IllegalArgumentException(ErrorCode.FILENAME_EXISTS.format(filename));
        }

        // Check for content duplicate by calculating hash
        String contentHash = fileStorageService.calculateHash(file.getInputStream());
        Optional<FileMetadataDto> existingByHash = fileMetadataService.checkDuplicateByHash(userId, contentHash);
        if (existingByHash.isPresent()) {
            throw new IllegalArgumentException(ErrorCode.CONTENT_EXISTS.format(existingByHash.get().getFileName()));
        }

    }

    /**
     * Validates pagination parameters
     */
    private void validatePaginationParameters(int page, int size) {
        ValidationUtils.validatePaginationParameters(page, size);
    }

    /**
     * Creates a Pageable object with sorting
     */
    private Pageable createPageable(int page, int size, SortBy sortBy, boolean ascending) {
        Sort sort = Sort.unsorted();

        if (sortBy != null) {
            String fieldName = mapSortByToFieldName(sortBy);
            sort = ascending ? Sort.by(fieldName).ascending() : Sort.by(fieldName).descending();
        }

        return PageRequest.of(page, size, sort);
    }

    /**
     * Maps SortBy enum to actual field names in the entity
     */
    private String mapSortByToFieldName(SortBy sortBy) {
        return switch (sortBy) {
            case FILENAME -> "fileName";
            case CONTENT_TYPE -> "contentType";
            case FILE_SIZE -> "size";
            case TAG -> "tags";
            default -> "uploadDate";
        };
    }

    /**
     * Creates a FileListResponse from paginated results
     */
    private FileListResponse createFileListResponse(List<FileMetadataDto> files, Page<FileMetadata> page) {
        FileListResponse response = new FileListResponse();
        response.setFiles(files);
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());

        return response;
    }
}