package com.erstorage.erstorageapi.auth;

public record IssuedToken(String value, long expiresInSeconds) {
}