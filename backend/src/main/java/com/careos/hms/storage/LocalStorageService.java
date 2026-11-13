package com.careos.hms.storage;

import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements DocumentStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp");

    private final Path rootStorageLocation;

    public LocalStorageService(@Value("${careos.storage.local-directory:uploads/documents}") String storagePath) {
        this.rootStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStorageLocation);
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to initialize document storage directory: " + e.getMessage());
        }
    }

    @Override
    public String storeFile(String hospitalPublicId, String originalFilename, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidRequestException("Cannot upload empty file");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new InvalidRequestException("File size exceeds 10MB limit");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidRequestException("File extension not permitted. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // Sanitize hospital public ID for path safety
        String safeHospitalDir = hospitalPublicId.replaceAll("[^a-zA-Z0-9_-]", "");
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path targetDir = rootStorageLocation.resolve(safeHospitalDir).normalize();
        Path targetFile = targetDir.resolve(storedFileName).normalize();

        // Path traversal validation
        if (!targetFile.startsWith(rootStorageLocation)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Invalid file destination path");
        }

        try {
            Files.createDirectories(targetDir);
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return safeHospitalDir + "/" + storedFileName;
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to write document to storage: " + e.getMessage());
        }
    }

    @Override
    public byte[] loadFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new InvalidRequestException("Invalid file key");
        }

        Path targetFile = rootStorageLocation.resolve(fileKey).normalize();
        if (!targetFile.startsWith(rootStorageLocation)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Unauthorized file path access attempt");
        }

        if (!Files.exists(targetFile)) {
            throw new ResourceNotFoundException("Document file not found on storage");
        }

        try {
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to read file from storage: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        Path targetFile = rootStorageLocation.resolve(fileKey).normalize();
        if (!targetFile.startsWith(rootStorageLocation)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Unauthorized file path access attempt");
        }
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            // Log and allow soft-delete to proceed
        }
    }

    @Override
    public boolean exists(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return false;
        }
        Path targetFile = rootStorageLocation.resolve(fileKey).normalize();
        return targetFile.startsWith(rootStorageLocation) && Files.exists(targetFile);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
    }
}