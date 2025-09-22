package com.github.simohin.file.storage.service.impl;

import com.github.simohin.file.storage.common.FileConstants;
import com.github.simohin.file.storage.dto.FileStorageResult;
import com.github.simohin.file.storage.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final Tika tika = new Tika();

    @Value("${app.file-storage.path:./storage}")
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("Created storage directory: {}", storageDir.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    @Override
    public FileStorageResult saveFile(UUID fileId, InputStream inputStream, String originalFileName) {
        log.info("Saving file with ID: {} and original name: {}", fileId, originalFileName);

        try {
            // Create organized directory structure
            Path filePath = getFilePath(fileId);
            Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Use streaming approach for large files - calculate hash while saving
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long fileSize;

            try (BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                 DigestInputStream digestInput = new DigestInputStream(bufferedInput, digest)) {

                // Save file while calculating hash in a single pass
                fileSize = Files.copy(digestInput, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String hash = HexFormat.of().formatHex(digest.digest());

            // Detect content type using a small sample from the saved file
            String contentType = detectContentTypeFromFile(filePath, originalFileName);

            log.info("File saved successfully: {} (size: {} bytes)", filePath.toAbsolutePath(), fileSize);

            return new FileStorageResult()
                    .setHash(hash)
                    .setSize(fileSize)
                    .setContentType(contentType);

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to save file with ID: {}", fileId, e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    public Optional<InputStream> getFile(UUID fileId) {
        log.debug("Getting file with ID: {}", fileId);

        try {
            Path filePath = getFilePath(fileId);
            if (!Files.exists(filePath)) {
                log.warn("File not found: {}", filePath.toAbsolutePath());
                return Optional.empty();
            }

            return Optional.of(Files.newInputStream(filePath));

        } catch (IOException e) {
            log.error("Failed to read file with ID: {}", fileId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteFile(UUID fileId) {
        log.info("Deleting file with ID: {}", fileId);

        try {
            Path filePath = getFilePath(fileId);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("File deleted successfully: {}", filePath.toAbsolutePath());
            } else {
                log.warn("File not found for deletion: {}", filePath.toAbsolutePath());
            }

            return deleted;

        } catch (IOException e) {
            log.error("Failed to delete file with ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    public String detectContentType(InputStream inputStream, String fileName) {
        try {
            // Read only a small sample for content type detection to avoid memory issues with large files
            byte[] sample = new byte[FileConstants.CONTENT_TYPE_SAMPLE_SIZE];

            // Mark the stream so we can read the sample and reset
            if (inputStream.markSupported()) {
                inputStream.mark(sample.length);
                int bytesRead = inputStream.read(sample);
                inputStream.reset();

                if (bytesRead <= 0) {
                    return "application/octet-stream";
                }

                // Use only the bytes that were actually read
                byte[] actualSample = bytesRead < sample.length ?
                        java.util.Arrays.copyOf(sample, bytesRead) : sample;

                return detectContentTypeFromBytes(actualSample, fileName);
            } else {
                // If mark is not supported, wrap in BufferedInputStream
                try (BufferedInputStream bufferedInput = new BufferedInputStream(inputStream)) {
                    bufferedInput.mark(sample.length);
                    int bytesRead = bufferedInput.read(sample);
                    bufferedInput.reset();

                    if (bytesRead <= 0) {
                        return "application/octet-stream";
                    }

                    byte[] actualSample = bytesRead < sample.length ?
                            java.util.Arrays.copyOf(sample, bytesRead) : sample;

                    return detectContentTypeFromBytes(actualSample, fileName);
                }
            }
        } catch (IOException e) {
            log.error("Failed to detect content type for file: {}", fileName, e);
            return "application/octet-stream";
        }
    }

    @Override
    public String calculateHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Use streaming approach for large files
            try (BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                 DigestInputStream digestInput = new DigestInputStream(bufferedInput, digest)) {

                // Read the stream in chunks to calculate hash without loading entire file into memory
                byte[] buffer = new byte[FileConstants.CONTENT_TYPE_SAMPLE_SIZE];
                while (digestInput.read(buffer) != -1) {
                    // Just consuming the stream to calculate the hash
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to calculate hash", e);
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    private Path getFilePath(UUID fileId) {
        // Create organized directory structure: first 2 chars / next 2 chars / fileId
        String fileIdStr = fileId.toString();
        String level1 = fileIdStr.substring(0, 2);
        String level2 = fileIdStr.substring(2, 4);
        return Paths.get(storagePath, level1, level2, fileIdStr);
    }

    private String calculateHashFromBytes(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String detectContentTypeFromBytes(byte[] fileBytes, String fileName) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
            String contentType = tika.detect(bais, fileName);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            log.error("Failed to detect content type for file: {}", fileName, e);
            return "application/octet-stream";
        }
    }

    private String detectContentTypeFromFile(Path filePath, String fileName) {
        try {
            // Read only a small sample for content type detection to avoid memory issues
            byte[] sample = new byte[FileConstants.CONTENT_TYPE_SAMPLE_SIZE];
            int bytesRead;

            try (InputStream fileInput = Files.newInputStream(filePath)) {
                bytesRead = fileInput.read(sample);
            }

            if (bytesRead <= 0) {
                return "application/octet-stream";
            }

            // Use only the bytes that were actually read
            byte[] actualSample = bytesRead < sample.length ?
                    java.util.Arrays.copyOf(sample, bytesRead) : sample;

            return detectContentTypeFromBytes(actualSample, fileName);

        } catch (IOException e) {
            log.error("Failed to detect content type from file: {}", fileName, e);
            return "application/octet-stream";
        }
    }
}