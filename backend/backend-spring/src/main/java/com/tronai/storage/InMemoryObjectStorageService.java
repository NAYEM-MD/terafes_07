package com.tronai.storage;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory object storage for the {@code test} profile so CV upload ITs do not require MinIO.
 * Production continues to use {@link MinioObjectStorageService}.
 */
@Service
@Profile("test")
@Primary
public class InMemoryObjectStorageService implements ObjectStorageService {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void putObject(String objectKey, byte[] data, String contentType) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        objects.put(objectKey, data == null ? new byte[0] : data.clone());
    }

    @Override
    public byte[] getObject(String objectKey) {
        byte[] data = objects.get(objectKey);
        if (data == null) {
            throw new IllegalStateException("Object not found: " + objectKey);
        }
        return data.clone();
    }

    @Override
    public void deleteObject(String objectKey) {
        objects.remove(objectKey);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void ensureBucketExists() {
        // no-op
    }
}
