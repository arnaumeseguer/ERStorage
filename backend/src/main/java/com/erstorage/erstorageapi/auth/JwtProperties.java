package com.erstorage.erstorageapi.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("erstorage.jwt")
public record JwtProperties(String secret,Duration expiration) {
}
