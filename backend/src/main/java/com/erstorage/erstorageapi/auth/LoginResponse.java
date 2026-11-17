package com.erstorage.erstorageapi.auth;

import com.erstorage.erstorageapi.user.UserResponse;

public record LoginResponse(
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}