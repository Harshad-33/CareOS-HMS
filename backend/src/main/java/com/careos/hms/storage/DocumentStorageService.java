package com.careos.hms.storage;

public interface DocumentStorageService {

    String storeFile(String hospitalPublicId, String originalFilename, byte[] content, String contentType);

    byte[] loadFile(String fileKey);

    void deleteFile(String fileKey);

    boolean exists(String fileKey);
}
