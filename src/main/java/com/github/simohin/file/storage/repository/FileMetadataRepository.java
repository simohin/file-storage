package com.github.simohin.file.storage.repository;

import com.github.simohin.file.storage.common.FileStatus;
import com.github.simohin.file.storage.common.Visibility;
import com.github.simohin.file.storage.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, UUID> {

    Optional<FileMetadata> findByIdAndStatus(UUID id, FileStatus status);


    List<FileMetadata> findByUserIdAndStatus(String userId, FileStatus status);

    List<FileMetadata> findByUserIdAndStatusAndVisibility(String userId, FileStatus status, Visibility visibility);

    Optional<FileMetadata> findByUserIdAndFileNameAndStatus(String userId, String fileName, FileStatus status);

    Optional<FileMetadata> findByUserIdAndHashAndStatus(String userId, String hash, FileStatus status);

    @Query("{'tags': {$in: ?0}, 'status': ?1}")
    List<FileMetadata> findByTagsInAndStatus(Set<String> tags, FileStatus status);

    @Query("{'userId': ?0, 'tags': {$in: ?1}, 'status': ?2}")
    List<FileMetadata> findByUserIdAndTagsInAndStatus(String userId, Set<String> tags, FileStatus status);

    @Query("{'visibility': 'PUBLIC', 'status': 'ACTIVE'}")
    List<FileMetadata> findPublicActiveFiles();

    List<FileMetadata> findByContentTypeContainingIgnoreCaseAndStatus(String contentType, FileStatus status);

    @Query("{'size': {$gte: ?0, $lte: ?1}, 'status': ?2}")
    List<FileMetadata> findBySizeRangeAndStatus(long minSize, long maxSize, FileStatus status);

    long countByUserIdAndStatus(String userId, FileStatus status);

    @Query(value = "{'userId': ?0, 'status': ?1}", count = true)
    long countActiveFilesByUserId(String userId, FileStatus status);

    // Paginated methods for listing
    @Query("{'visibility': 'PUBLIC', 'status': 'ACTIVE'}")
    Page<FileMetadata> findPublicActiveFiles(Pageable pageable);

    @Query("{'visibility': 'PUBLIC', 'status': 'ACTIVE', 'tags': {$in: ?0}}")
    Page<FileMetadata> findPublicActiveFilesByTags(Set<String> tags, Pageable pageable);

    Page<FileMetadata> findByUserIdAndStatus(String userId, FileStatus status, Pageable pageable);

    Page<FileMetadata> findByUserIdAndStatusAndVisibility(String userId, FileStatus status, Visibility visibility, Pageable pageable);

    @Query("{'userId': ?0, 'status': ?1, 'tags': {$in: ?2}}")
    Page<FileMetadata> findByUserIdAndStatusAndTagsIn(String userId, FileStatus status, Set<String> tags, Pageable pageable);

    @Query("{'userId': ?0, 'status': ?1, 'visibility': ?2, 'tags': {$in: ?3}}")
    Page<FileMetadata> findByUserIdAndStatusAndVisibilityAndTagsIn(String userId, FileStatus status, Visibility visibility, Set<String> tags, Pageable pageable);
}