package com.erstorage.erstorageapi.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<FileEntity> findByOwnerIdAndFolderIsNullOrderByNameAsc(UUID ownerId);

    List<FileEntity> findByOwnerIdAndFolderIdOrderByNameAsc(UUID ownerId, UUID folderId);

    @Query("""
            select new com.erstorage.erstorageapi.file.StoredObject(f.storageKey, f.sizeBytes)
              from FileEntity f
             where f.ownerId = :ownerId
               and f.folder.id in :folderIds
            """)
    List<StoredObject> findStoredObjectsInFolders(@Param("ownerId") UUID ownerId,
                                                  @Param("folderIds") Collection<UUID> folderIds);

    @Query("select f.storageKey from FileEntity f where f.ownerId in :ownerIds")
    List<String> findStorageKeysByOwnerIds(@Param("ownerIds") Collection<UUID> ownerIds);

    boolean existsByOwnerIdAndFolderIsNullAndName(UUID ownerId, String name);

    boolean existsByOwnerIdAndFolderIdAndName(UUID ownerId, UUID folderId, String name);
}