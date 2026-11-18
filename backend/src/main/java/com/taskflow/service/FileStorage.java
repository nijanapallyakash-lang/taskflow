package com.taskflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.taskflow.web.ApiExceptionHandler.badRequest;

/**
 * Stores uploads on local disk under random names (no path traversal possible).
 * ponytail: local disk is ephemeral on Render; swap for S3/R2 if attachments must survive redeploys.
 */
@Component
public class FileStorage {
    private final Path root;

    public FileStorage(@Value("${app.upload-dir}") String dir) throws IOException {
        root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("File is empty");
        String name = UUID.randomUUID().toString();
        try {
            Files.copy(file.getInputStream(), root.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return name;
    }

    public Resource load(String name) {
        return new FileSystemResource(root.resolve(name));
    }

    public void delete(String name) {
        try {
            if (name != null) Files.deleteIfExists(root.resolve(name));
        } catch (IOException ignored) {
            // orphaned file on disk is harmless
        }
    }
}
