package com.github.simohin.file.storage.service;

import com.github.simohin.file.storage.dto.FileStorageResult;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface FileStorageService {

    FileStorageResult saveFile(UUID fileId, InputStream inputStream, String originalFileName);

    Optional<InputStream> getFile(UUID fileId);

    boolean deleteFile(UUID fileId);

    String detectContentType(InputStream inputStream, String fileName);

    String calculateHash(InputStream inputStream);
}