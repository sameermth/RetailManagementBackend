package com.retailmanagement.common.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public String storeFile(MultipartFile file) {
        return storeFile(file, "general");
    }

    @Override
    public String storeFile(MultipartFile file, String directory) {
        try {
            // Create directory if not exists
            Path targetDir = Paths.get(uploadDir).resolve(directory);
            Files.createDirectories(targetDir);

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + extension;

            // Copy file to target location
            Path targetLocation = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", filename);
            return directory + "/" + filename;

        } catch (IOException ex) {
            throw new BusinessException("Could not store file. Please try again!", ex.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        return loadFileAsResource(fileName, "general");
    }

    @Override
    public Resource loadFileAsResource(String fileName, String directory) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(directory).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new BusinessException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new BusinessException("File not found: " + fileName, ex.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileName) {
        deleteFile(fileName, "general");
    }

    @Override
    public void deleteFile(String fileName, String directory) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(directory).resolve(fileName);
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            throw new BusinessException("Could not delete file: " + fileName, ex.getMessage());
        }
    }

    @Override
    public List<String> listFiles(String directory) {
        try (Stream<Path> walk = Files.walk(Paths.get(uploadDir).resolve(directory))) {
            return walk.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new BusinessException("Could not list files in directory: " + directory, ex.getMessage());
        }
    }

    @Override
    public Path getFilePath(String fileName) {
        return getFilePath(fileName, "general");
    }

    private Path getFilePath(String fileName, String directory) {
        return Paths.get(uploadDir).resolve(directory).resolve(fileName);
    }

    @Override
    public boolean fileExists(String fileName) {
        return fileExists(fileName, "general");
    }

    private boolean fileExists(String fileName, String directory) {
        return Files.exists(getFilePath(fileName, directory));
    }

    @Override
    public long getFileSize(String fileName) {
        try {
            Path filePath = getFilePath(fileName);
            return Files.size(filePath);
        } catch (IOException ex) {
            throw new BusinessException("Could not get file size: " + fileName, ex.getMessage());
        }
    }

    @Override
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}