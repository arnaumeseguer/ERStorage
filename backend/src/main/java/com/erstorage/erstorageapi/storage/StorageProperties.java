package com.erstorage.erstorageapi.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("erstorage.storage")
public record StorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        boolean createBucketOnStartup
) {
}
