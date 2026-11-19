package com.erstorage.erstorageapi.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("erstorage.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
