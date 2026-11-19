package com.erstorage.erstorageapi.folder;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    // --------------> Reading <--------------

    @Transactional(readOnly = true)
    public FolderContentsResponse getFolderContents(UUID ownerId) {
        List<FolderResponse> folders = folderRepository
                .findByOwnerIdAndParentIsNullOrderByNameAsc(ownerId)
                .stream()
                .map(FolderResponse::from)
                .toList();

        return new FolderContentsResponse(null, List.of(), folders);
    }

    @Transactional(readOnly = true)
    public FolderContentsResponse getContents(UUID ownerId, UUID folderId) {
        Folder folder = findOwned(ownerId, folderId);

        List<FolderResponse> subfolders = folderRepository
                .findByOwnerIdAndParentIdOrderByNameAsc(ownerId, folder.getId())
                .stream()
                .map(FolderResponse::from)
                .toList();

        return new FolderContentsResponse(FolderResponse.from(folder), buildBreadcrumbs(folder), subfolders);
    }

    // --------------> changing <--------------

    @Transactional
    public FolderResponse create(UUID ownerId, CreateFolderRequest request) {
        String name = request.name().trim();
        Folder parent = request.parentId() == null ? null : findOwned(ownerId, request.parentId());

        ensureNameAvailable(ownerId, parent, name);

        Folder saved = folderRepository.save(new Folder(ownerId, parent, name));
        return FolderResponse.from(saved);
    }

    @Transactional
    public FolderResponse rename(UUID ownerId, UUID folderId, RenameFolderRequest request) {
        Folder folder = findOwned(ownerId, folderId);
        String newName = request.name().trim();

        if (newName.equals(folder.getName())) {
            return FolderResponse.from(folder);
        }

        ensureNameAvailable(ownerId, folder.getParent(), newName);
        folder.rename(newName);
        return FolderResponse.from(folder);
    }

    @Transactional
    public FolderResponse move(UUID ownerId, UUID folderId, MoveFolderRequest request) {
        Folder folder = findOwned(ownerId, folderId);
        Folder newParent = request.parentId() == null ? null : findOwned(ownerId, request.parentId());

        if (isSelfOrDescendant(folder, newParent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A folder cannot be moved into itself or none of its subfolders");
        }

        ensureNameAvailable(ownerId, newParent, folder.getName());
        folder.moveTo(newParent);
        return FolderResponse.from(folder);
    }

    @Transactional
    public void delete(UUID ownerId, UUID folderId) {
        Folder folder = findOwned(ownerId, folderId);
        folderRepository.delete(folder);
    }

    // --------------> helpers <--------------

    private Folder findOwned(UUID ownerId, UUID folderId) {
        return folderRepository.findByIdAndOwnerId(folderId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
    }

    private void ensureNameAvailable(UUID ownerId, Folder parent, String name) {
        boolean taken = parent == null
                ? folderRepository.existsByOwnerIdAndParentIsNullAndName(ownerId, name)
                : folderRepository.existsByOwnerIdAndParentIdAndName(ownerId, parent.getId(), name);

        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder named '" + name + "' already exists");
        }
    }

    private List<BreadcrumbItem> buildBreadcrumbs(Folder folder) {
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            breadcrumbs.addFirst(new BreadcrumbItem(current.getId(), current.getName()));
            current = current.getParent();
        }
        return breadcrumbs;
    }

    private boolean isSelfOrDescendant(Folder folder, Folder target) {
        Folder current = target;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
