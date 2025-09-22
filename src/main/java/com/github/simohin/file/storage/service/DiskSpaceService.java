package com.github.simohin.file.storage.service;

import com.github.simohin.file.storage.config.DiskSpaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiskSpaceService {

    private final DiskSpaceProperties diskSpaceProperties;

    public void validateDiskSpace(long additionalSizeBytes) {
        if (!diskSpaceProperties.isDiskSpaceCheckEnabled()) {
            return;
        }

        Path storagePath = Paths.get(diskSpaceProperties.getPath());

        try {
            long currentUsage = getCurrentStorageUsage(storagePath);
            long maxAllowedBytes = diskSpaceProperties.getMaxTotalSize().toBytes();
            long projectedUsage = currentUsage + additionalSizeBytes;

            log.debug("Disk space check: current={} bytes, additional={} bytes, projected={} bytes, limit={} bytes",
                    currentUsage, additionalSizeBytes, projectedUsage, maxAllowedBytes);

            if (projectedUsage > maxAllowedBytes) {
                throw new DiskSpaceException(
                        String.format("Storage limit exceeded. Current: %d bytes, Adding: %d bytes, Limit: %d bytes (%s)",
                                currentUsage, additionalSizeBytes, maxAllowedBytes,
                                diskSpaceProperties.getMaxTotalSize().toString())
                );
            }

            double usagePercentage = (double) projectedUsage / maxAllowedBytes * 100;
            if (usagePercentage > diskSpaceProperties.getDiskSpaceThreshold()) {
                log.warn("Disk space usage approaching limit: {:.1f}% of {} ({} bytes)",
                        usagePercentage, diskSpaceProperties.getMaxTotalSize(), projectedUsage);
            }

        } catch (IOException e) {
            log.error("Failed to check disk space usage", e);
            throw new DiskSpaceException("Unable to verify disk space availability", e);
        }
    }

    public DiskSpaceInfo getDiskSpaceInfo() {
        Path storagePath = Paths.get(diskSpaceProperties.getPath());

        try {
            long currentUsage = getCurrentStorageUsage(storagePath);
            long maxAllowedBytes = diskSpaceProperties.getMaxTotalSize().toBytes();
            long availableBytes = maxAllowedBytes - currentUsage;
            double usagePercentage = (double) currentUsage / maxAllowedBytes * 100;

            return DiskSpaceInfo.builder()
                    .currentUsageBytes(currentUsage)
                    .maxAllowedBytes(maxAllowedBytes)
                    .availableBytes(Math.max(0, availableBytes))
                    .usagePercentage(usagePercentage)
                    .isNearLimit(usagePercentage > diskSpaceProperties.getDiskSpaceThreshold())
                    .storagePath(storagePath.toAbsolutePath().toString())
                    .build();

        } catch (IOException e) {
            log.error("Failed to get disk space info", e);
            return DiskSpaceInfo.builder()
                    .currentUsageBytes(-1)
                    .maxAllowedBytes(diskSpaceProperties.getMaxTotalSize().toBytes())
                    .availableBytes(-1)
                    .usagePercentage(-1)
                    .isNearLimit(false)
                    .storagePath(storagePath.toAbsolutePath().toString())
                    .error("Unable to determine disk space usage")
                    .build();
        }
    }

    private long getCurrentStorageUsage(Path storagePath) throws IOException {
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
            return 0;
        }

        return Files.walk(storagePath)
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        log.warn("Failed to get size of file: {}", file, e);
                        return 0;
                    }
                })
                .sum();
    }

    public static class DiskSpaceException extends RuntimeException {
        public DiskSpaceException(String message) {
            super(message);
        }

        public DiskSpaceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class DiskSpaceInfo {
        private final long currentUsageBytes;
        private final long maxAllowedBytes;
        private final long availableBytes;
        private final double usagePercentage;
        private final boolean isNearLimit;
        private final String storagePath;
        private final String error;
    }
}