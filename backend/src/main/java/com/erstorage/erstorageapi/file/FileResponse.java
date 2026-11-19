package com.erstorage.erstorageapi.file;

import java.io.FileReader;
import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String name,
        UUID folderId,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        Instant updatedAt
) {
    public static FileResponse from(FileEntity file) {
        UUID folderId = file.getFolder() == null ? null : file.getFolder().getId();
        return new FileResponse(
                file.getId(),
                file.getName(),
                folderId,
                file.getContentType(),
                file.getSizeBytes(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}
