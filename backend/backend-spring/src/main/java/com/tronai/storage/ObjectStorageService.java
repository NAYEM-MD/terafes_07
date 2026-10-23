package com.tronai.storage;

public interface ObjectStorageService {

    /**
     * Upload an object to private storage.
     */
    void putObject(String objectKey, byte[] data, String contentType);

    /**
     * Retrieve raw object bytes from private storage.
     */
    byte[] getObject(String objectKey);

    /**
     * Delete an object from private storage.
     */
    void deleteObject(String objectKey);

    /**
     * Check if storage backend is reachable and ready.
     */
    boolean isAvailable();

    /**
     * Ensure the target private bucket exists.
     */
    void ensureBucketExists();
}
