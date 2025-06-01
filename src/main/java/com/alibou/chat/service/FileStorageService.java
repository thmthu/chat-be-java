package com.alibou.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

   public void storeFile(MultipartFile file, String fileId) throws IOException {
    // Normalize file name
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    
    // Check if the file's name contains invalid characters
    if (originalFileName.contains("..")) {
        throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
    }
    
    // Store with the fileId as the filename (preserving extension)
    String fileExtension = "";
    if (originalFileName.contains(".")) {
        fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    
    // Copy file to the target location
    Path targetLocation = this.fileStorageLocation.resolve(fileId + fileExtension);
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
}
public Path getFilePath(String fileId) throws IOException {
    // First try to find a file with the exact fileId as name
    Path exactPath = fileStorageLocation.resolve(fileId);
    if (Files.exists(exactPath)) {
        return exactPath;
    }
    
    // If not found, look for files that have the fileId as prefix (with extension)
    try (Stream<Path> files = Files.list(fileStorageLocation)) {
        Optional<Path> foundFile = files
                .filter(path -> path.getFileName().toString().startsWith(fileId))
                .findFirst();
        
        if (foundFile.isPresent()) {
            return foundFile.get();
        }
    }
    
    throw new IOException("File not found: " + fileId);
}
}