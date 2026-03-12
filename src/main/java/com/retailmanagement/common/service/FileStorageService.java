package com.retailmanagement.common.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    String storeFile(MultipartFile file, String directory);
    Resource loadFileAsResource(String fileName);
    Resource loadFileAsResource(String fileName, String directory);
    void deleteFile(String fileName);
    void deleteFile(String fileName, String directory);
    List<String> listFiles(String directory);
    Path getFilePath(String fileName);
    boolean fileExists(String fileName);
    long getFileSize(String fileName);
    String getFileExtension(String fileName);
}