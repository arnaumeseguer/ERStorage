package com.erstorage.erstorageapi.demo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "erstorage.demo")
public record DemoProperties(
        boolean enabled,
        @NotNull Duration lifetime,
        @NotNull DataSize quota,
        @NotNull DataSize maxFileSize,
        @Positive int maxActiveUsers,
        @Positive int maxStartsPerIpPerHour
) {
}