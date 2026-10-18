package com.docbridge.backend.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${app.storage.local-dir:./data/storage}") String localDir) {
        this.basePath = Paths.get(localDir).toAbsolutePath();
    }

    @Override
    public String save(byte[] content, String filename) {
        String safeName = filename == null ? "file.bin" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = LocalDate.now() + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = basePath.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("保存文件失败", e);
        }
    }

    @Override
    public byte[] read(String key) {
        Path target = basePath.resolve(key);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败", e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(basePath.resolve(key));
        } catch (IOException ignored) {
            // ignore cleanup failures in MVP
        }
    }
}

