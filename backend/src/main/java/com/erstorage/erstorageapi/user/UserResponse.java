package com.erstorage.erstorageapi.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        long storageUsedBytes,
        long storageQuotaBytes,
        boolean guest,
        Instant expiresAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStorageUsedBytes(),
                user.getStorageQuotaBytes(),
                user.isGuest(),
                user.getExpiresAt()
        );
    }
}