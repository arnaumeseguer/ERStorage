package com.erstorage.erstorageapi.common;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "erstorage.limits")
public record LimitsProperties(
        boolean registrationEnabled,
        @NotNull DataSize totalStorage
) {
}