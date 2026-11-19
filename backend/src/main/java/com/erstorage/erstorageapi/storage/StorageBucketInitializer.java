package com.erstorage.erstorageapi.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;


@Component
public class StorageBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageBucketInitializer.class);

    private final S3Client s3Client;
    private final StorageProperties properties;

    public StorageBucketInitializer(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.createBucketOnStartup()) {
            return;
        }

        String bucket = properties.bucket();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Storage bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created storage bucket '{}'", bucket);
        }
    }
}
