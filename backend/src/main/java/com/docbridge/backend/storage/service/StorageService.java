package com.docbridge.backend.storage.service;

public interface StorageService {

    String save(byte[] content, String filename);

    byte[] read(String key);

    void delete(String key);
}

