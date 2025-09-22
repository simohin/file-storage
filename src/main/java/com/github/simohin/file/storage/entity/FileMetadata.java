package com.github.simohin.file.storage.entity;

import com.github.simohin.file.storage.common.FileStatus;
import com.github.simohin.file.storage.common.Visibility;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Accessors(chain = true)
@Document(collection = "files")
@CompoundIndex(name = "user_filename_idx",
        def = "{'userId': 1, 'fileName': 1}",
        partialFilter = "{'status': { $in: ['PENDING', 'ACTIVE'] }}",
        unique = true)
@CompoundIndex(name = "user_sha256_hash_idx",
        def = "{'userId': 1, 'hash': 1}",
        partialFilter = "{'status': { $eq: 'ACTIVE' }}",
        unique = true)
@CompoundIndex(name = "visibility_status_idx",
        def = "{'visibility': 1, 'status': 1}",
        partialFilter = "{'status': { $eq: 'ACTIVE' }}")
@CompoundIndex(name = "userId_status_idx",
        def = "{'userId': 1, 'status': 1}")
public class FileMetadata {

    @Id
    private UUID id;

    private String fileName;
    private String userId;
    private Visibility visibility;
    private Set<String> tags;
    private LocalDateTime uploadDate;
    private String contentType;
    private long size;
    private String hash;
    private FileStatus status;

}