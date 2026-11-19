package com.erstorage.erstorageapi.storage;

import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class S3StorageService implements StorageService {

    private static final int MAX_KEYS_PER_DELETE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.bucket();
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(content, size));
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        List<ObjectIdentifier> batch = new ArrayList<>();

        for (String key : keys) {
            batch.add(ObjectIdentifier.builder().key(key).build());
            if (batch.size() >= MAX_KEYS_PER_DELETE) {
                deleteBatch(batch);
                batch = new ArrayList<>();
            }
        }

        if (!batch.isEmpty()) {
            deleteBatch(batch);
        }
    }

    @Override
    public String createDownloadUrl(String key, String fileName, Duration validFor) {
        String contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition(contentDisposition)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(validFor)
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void deleteBatch(List<ObjectIdentifier> batch) {
        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(batch).build())
                .build());
    }
}
