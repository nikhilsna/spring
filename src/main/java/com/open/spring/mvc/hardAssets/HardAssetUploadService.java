package com.open.spring.mvc.hardAssets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HardAssetUploadService {

    private static final String UPLOAD_DIR = "./uploads/";

    public UploadResult upload(MultipartFile file, String uid) {
        if (file == null || file.isEmpty()) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Please select a file to upload.");
        }

        Path uploadPath = ensureUploadDirectory();
        String originalFilename = file.getOriginalFilename();
        String baseName = sanitizeFilename(originalFilename);
        String localFileUUID = java.util.UUID.randomUUID().toString() + "_" + baseName;
        Path filePath = resolveSafeTargetPath(uploadPath, localFileUUID);
        copyFile(file, filePath);

        return new UploadResult(originalFilename, localFileUUID, uid);
    }

    private Path ensureUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            return uploadPath;
        } catch (IOException e) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare upload directory: " + e.getMessage(), e);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }

        String baseName = Paths.get(originalFilename).getFileName().toString();
        if (baseName.contains("..") || baseName.contains("/") || baseName.contains("\\")) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }
        return baseName;
    }

    private Path resolveSafeTargetPath(Path uploadPath, String localFileUUID) {
        Path filePath = uploadPath.resolve(localFileUUID).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid target path.");
        }
        return filePath;
    }

    private void copyFile(MultipartFile file, Path filePath) {
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage(), e);
        }
    }

    public static class UploadResult {
        private final String originalFilename;
        private final String localFileUUID;
        private final String uid;

        public UploadResult(String originalFilename, String localFileUUID, String uid) {
            this.originalFilename = originalFilename;
            this.localFileUUID = localFileUUID;
            this.uid = uid;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getLocalFileUUID() {
            return localFileUUID;
        }

        public String getUid() {
            return uid;
        }
    }

    public static class UploadException extends RuntimeException {
        private final HttpStatus status;

        public UploadException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public UploadException(HttpStatus status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}