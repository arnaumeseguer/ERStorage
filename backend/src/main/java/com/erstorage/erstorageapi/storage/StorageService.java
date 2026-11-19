package com.erstorage.erstorageapi.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;

public interface StorageService {

    void store(String key, InputStream content, long size, String contentType);

    void delete(String key);

    void deleteAll(Collection<String> keys);

    String createDownloadUrl(String key, String fileName, Duration validFor);
}
