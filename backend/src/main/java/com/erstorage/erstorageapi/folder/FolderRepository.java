package com.erstorage.erstorageapi.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    Optional<Folder> findByIDAndOwnerID(UUID id, UUID ownerID);

    List<Folder> findByOwnerIDAndParentIsNullOrderByNameAsc(UUID ownerID);

    List<Folder> findByOwnerIdAndParentIdOrderByNameAsc(UUID ownerID, UUID parentID);

    boolean existsByOwnerIdAndParentIsNullAndName(UUID ownerId, String name);

    boolean existsByOwnerIdAndParentIdAndName(UUID ownerId, UUID parentId, String name);

}
