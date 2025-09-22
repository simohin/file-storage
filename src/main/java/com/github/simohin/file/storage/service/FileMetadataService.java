package com.github.simohin.file.storage.service;

import com.github.simohin.file.storage.common.FileStatus;
import com.github.simohin.file.storage.common.Visibility;
import com.github.simohin.file.storage.dto.FileMetadataDto;
import com.github.simohin.file.storage.entity.FileMetadata;
import com.github.simohin.file.storage.mapper.FileMetadataMapper;
import com.github.simohin.file.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;

    @Transactional
    public FileMetadataDto createFileMetadata(UUID fileId, String fileName, String userId, String contentType,
                                              long size, String hash, Visibility visibility, Set<String> tags) {

        FileMetadata metadata = new FileMetadata()
                .setId(fileId)
                .setFileName(fileName)
                .setUserId(userId)
                .setContentType(contentType)
                .setSize(size)
                .setHash(hash)
                .setVisibility(visibility)
                .setTags(tags)
                .setUploadDate(LocalDateTime.now())
                .setStatus(FileStatus.PENDING);

        FileMetadata saved = fileMetadataRepository.save(metadata);

        return fileMetadataMapper.toDto(saved);
    }

    @Transactional
    public Optional<FileMetadataDto> updateFileStatus(UUID fileId, FileStatus status) {

        Optional<FileMetadata> metadata = fileMetadataRepository.findById(fileId);
        if (metadata.isPresent()) {
            FileMetadata updated = metadata.get().setStatus(status);
            FileMetadata saved = fileMetadataRepository.save(updated);
            return Optional.of(fileMetadataMapper.toDto(saved));
        }

        log.warn("File not found for ID: {}", fileId);
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<FileMetadataDto> getFileMetadata(UUID fileId) {
        return fileMetadataRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE)
                .map(fileMetadataMapper::toDto);
    }


    @Transactional(readOnly = true)
    public Optional<FileMetadataDto> checkFileExists(String userId, String fileName) {
        return fileMetadataRepository.findByUserIdAndFileNameAndStatus(userId, fileName, FileStatus.ACTIVE)
                .map(fileMetadataMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<FileMetadataDto> checkDuplicateByHash(String userId, String hash) {
        return fileMetadataRepository.findByUserIdAndHashAndStatus(userId, hash, FileStatus.ACTIVE)
                .map(fileMetadataMapper::toDto);
    }


    @Transactional
    public boolean deleteFileMetadata(UUID fileId) {

        Optional<FileMetadata> metadata = fileMetadataRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE);
        if (metadata.isPresent()) {
            FileMetadata updated = metadata.get().setStatus(FileStatus.DELETED);
            fileMetadataRepository.save(updated);
            return true;
        }

        log.warn("File not found for deletion, ID: {}", fileId);
        return false;
    }

    @Transactional
    public Optional<FileMetadataDto> updateFileMetadata(UUID fileId, FileMetadataDto updateDto) {

        Optional<FileMetadata> metadata = fileMetadataRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE);
        if (metadata.isPresent()) {
            FileMetadata existing = metadata.get();

            if (updateDto.getFileName() != null) {
                existing.setFileName(updateDto.getFileName());
            }
            if (updateDto.getVisibility() != null) {
                existing.setVisibility(updateDto.getVisibility());
            }
            if (updateDto.getTags() != null) {
                existing.setTags(updateDto.getTags());
            }

            FileMetadata saved = fileMetadataRepository.save(existing);
            return Optional.of(fileMetadataMapper.toDto(saved));
        }

        log.warn("File not found for update, ID: {}", fileId);
        return Optional.empty();
    }


}