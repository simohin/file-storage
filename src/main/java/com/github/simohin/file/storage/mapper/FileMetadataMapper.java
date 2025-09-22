package com.github.simohin.file.storage.mapper;

import com.github.simohin.file.storage.dto.FileMetadataDto;
import com.github.simohin.file.storage.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FileMetadataMapper {

    FileMetadataMapper INSTANCE = Mappers.getMapper(FileMetadataMapper.class);

    FileMetadataDto toDto(FileMetadata entity);

    @Mapping(target = "id", ignore = true)
    FileMetadata toEntity(FileMetadataDto dto);
}