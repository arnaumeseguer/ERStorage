package com.erstorage.erstorageapi.file;

import com.erstorage.erstorageapi.common.LimitsProperties;
import com.erstorage.erstorageapi.demo.DemoProperties;
import com.erstorage.erstorageapi.folder.Folder;
import com.erstorage.erstorageapi.folder.FolderRepository;
import com.erstorage.erstorageapi.storage.StorageService;
import com.erstorage.erstorageapi.user.User;
import com.erstorage.erstorageapi.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Duration DOWNLOAD_LINK_VALIDITY = Duration.ofMinutes(5);
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "msi", "com", "scr", "ps1", "vbs", "sh"
    );
    private static final Set<String> DEMO_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "pdf", "txt", "md"
    );

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final LimitsProperties limits;
    private final DemoProperties demo;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository,
                       UserRepository userRepository, StorageService storageService,
                       LimitsProperties limits, DemoProperties demo) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.limits = limits;
        this.demo = demo;
    }

    // ---------- upload ----------

    @Transactional
    public FileResponse upload(UUID ownerId, UUID folderId, MultipartFile upload) {
        if (upload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        String name = cleanFileName(upload.getOriginalFilename());
        ensureAllowedType(name);

        long size = upload.getSize();
        if (findOwner(ownerId).isGuest()) {
            ensureDemoSize(size);
            ensureDemoType(name);
        }

        Folder folder = folderId == null ? null : findOwnedFolder(ownerId, folderId);
        ensureNameAvailable(ownerId, folder, name);

        long totalLimit = limits.totalStorage().toBytes();
        if (userRepository.reserveStorage(ownerId, size, totalLimit) == 0) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "Not enough space to upload");
        }

        String storageKey = "users/" + ownerId + "/" + UUID.randomUUID();
        String contentType = upload.getContentType() == null ? DEFAULT_CONTENT_TYPE : upload.getContentType();

        try (InputStream content = upload.getInputStream()) {
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

    // ---------- download ----------

    @Transactional(readOnly = true)
    public DownloadUrlResponse getDownloadUrl(UUID ownerId, UUID fileId) {
        FileEntity file = findOwnedFile(ownerId, fileId);
        String url = storageService.createDownloadUrl(file.getStorageKey(), file.getName(), DOWNLOAD_LINK_VALIDITY);
        return new DownloadUrlResponse(url, DOWNLOAD_LINK_VALIDITY.toSeconds());
    }

    // ---------- rename / move ----------

    @Transactional
    public FileResponse rename(UUID ownerId, UUID fileId, RenameFileRequest request) {
        FileEntity file = findOwnedFile(ownerId, fileId);
        String newName = request.name().trim();

        if (newName.equals(file.getName())) {
            return FileResponse.from(file);
        }

        if (newName.contains("/") || newName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File names cannot contain / or \\");
        }
        ensureAllowedType(newName);
        if (findOwner(ownerId).isGuest()) {
            ensureDemoType(newName);
        }
        ensureNameAvailable(ownerId, file.getFolder(), newName);

        file.rename(newName);
        return FileResponse.from(file);
    }

    @Transactional
    public FileResponse move(UUID ownerId, UUID fileId, MoveFileRequest request) {
        FileEntity file = findOwnedFile(ownerId, fileId);
        Folder target = request.folderId() == null ? null : findOwnedFolder(ownerId, request.folderId());

        UUID currentFolderId = file.getFolder() == null ? null : file.getFolder().getId();
        if (Objects.equals(currentFolderId, request.folderId())) {
            return FileResponse.from(file);
        }

        ensureNameAvailable(ownerId, target, file.getName());
        file.moveTo(target);
        return FileResponse.from(file);
    }

    // ---------- delete ----------

    @Transactional
    public void delete(UUID ownerId, UUID fileId) {
        FileEntity file = findOwnedFile(ownerId, fileId);

        fileRepository.delete(file);
        fileRepository.flush();
        userRepository.releaseStorage(ownerId, file.getSizeBytes());
        storageService.delete(file.getStorageKey());
    }

    // ---------- helpers ----------

    private User findOwner(UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private FileEntity findOwnedFile(UUID ownerId, UUID fileId) {
        return fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The file name is longer than " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureAllowedType(String name) {
        String extension = extensionOf(name);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Files of type ." + extension + " are not allowed");
        }
    }

    private void ensureDemoSize(long size) {
        if (size > demo.maxFileSize().toBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Demo uploads are limited to " + demo.maxFileSize().toMegabytes() + " MB per file");
        }
    }

    private void ensureDemoType(String name) {
        if (!DEMO_EXTENSIONS.contains(extensionOf(name))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The demo only accepts images, PDFs and text files");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A file named '" + name + "' already exists here");
        }
    }
}