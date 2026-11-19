package com.erstorage.erstorageapi.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<FileEntity> findByOwnerIdAndFolderIsNullOrderByNameAsc(UUID ownerId);

    List<FileEntity> findByOwnerIdAndFolderIdOrderByNameAsc(UUID ownerId, UUID folderId);

    boolean existsByOwnerIdAndFolderIsNullAndName(UUID ownerId, String name);

    boolean existsByOwnerIdAndFolderIdAndName(UUID ownerId, UUID folderId, String name);
}
