package com.aiproof.studycompanion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir) {

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create upload directory",
                    e
            );
        }
    }

    public String storeFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        String originalFileName =
                StringUtils.cleanPath(
                        file.getOriginalFilename() == null
                                ? "unknown-file"
                                : file.getOriginalFilename()
                );

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid file name: " + originalFileName
            );
        }

        String extension = "";

        int extensionIndex =
                originalFileName.lastIndexOf(".");

        if (extensionIndex >= 0) {
            extension =
                    originalFileName.substring(extensionIndex)
                            .toLowerCase();
        }

        String storedFileName =
                UUID.randomUUID() + extension;

        Path targetLocation =
                uploadDirectory.resolve(storedFileName)
                        .normalize();

        if (!targetLocation.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid file path"
            );
        }

        try (InputStream inputStream = file.getInputStream()) {

            Files.copy(
                    inputStream,
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not store file: " + originalFileName,
                    e
            );
        }

        return targetLocation.toString();
    }

    public void deleteFile(String filePath) {

        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {

            Path path =
                    Paths.get(filePath)
                            .toAbsolutePath()
                            .normalize();

            if (path.startsWith(uploadDirectory)) {
                Files.deleteIfExists(path);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not delete file: " + filePath,
                    e
            );
        }
    }
}