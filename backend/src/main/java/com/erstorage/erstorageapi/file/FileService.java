package com.erstorage.erstorageapi.file;

import com.erstorage.erstorageapi.folder.Folder;
import com.erstorage.erstorageapi.folder.FolderRepository;
import com.erstorage.erstorageapi.storage.StorageService;
import com.erstorage.erstorageapi.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "msi", "com", "scr", "ps1", "vbs", "sh"
    );

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository,
                       UserRepository userRepository, StorageService storageService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public FileResponse upload(UUID ownerId, UUID folderId, MultipartFile upload) {
        if (upload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        String name = cleanFileName(upload.getOriginalFilename());
        ensureAllowedType(name);

        Folder folder = folderId == null ? null : findOwnedFolder(ownerId, folderId);
        ensureNameAvailable(ownerId, folder, name);

        long size = upload.getSize();
        if (userRepository.reserveStorage(ownerId, size) == 0) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "Not enough space to upload");
        }

        String storageKey = "users/" + ownerId + "/" + UUID.randomUUID();
        String contentType = upload.getContentType() == null ? DEFAULT_CONTENT_TYPE : upload.getContentType();

        try (InputStream content = upload.getInputStream()){
            storageService.store(storageKey, content, size, contentType);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the uploaded file", exception);
        }

        try {
            FileEntity saved = fileRepository.saveAndFlush(
                    new FileEntity(ownerId, folder, name, storageKey, contentType, size));
            return FileResponse.from(saved);
        } catch (RuntimeException exception) {
            storageService.delete(storageKey);
            throw exception;
        }
    }

    // --------------> helpers <--------------

    private String cleanFileName(String originalName) {
        if (originalName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file has no name");
        }

        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();

        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file has no name");
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file name can't be longer than " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    private void ensureAllowedType(String name) {
        int dot = name.lastIndexOf('.');
        if (dot == -1) {
            return;
        }
        String extension = name.substring(dot+1).toLowerCase(Locale.ROOT);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Files with the ."+ extension + " extension are not allowed");
        }
    }

    private Folder findOwnedFolder(UUID ownerId, UUID folderId) {
        return folderRepository.findByIdAndOwnerId(folderId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
    }

    private void ensureNameAvailable(UUID ownerId, Folder folder, String name) {
        boolean taken = folder == null
                ? fileRepository.existsByOwnerIdAndFolderIsNullAndName(ownerId, name)
                : fileRepository.existsByOwnerIdAndFolderIdAndName(ownerId, folder.getId(), name);
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder named '" + name + "' already exists");
        }
    }
}
