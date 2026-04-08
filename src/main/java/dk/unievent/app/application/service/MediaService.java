package dk.unievent.app.application.service;

import dk.unievent.app.infrastructure.client.SeaweedFsClient;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class MediaService {

    private final SeaweedFsClient seaweedClient;

    public MediaService(SeaweedFsClient seaweedClient) {
        this.seaweedClient = seaweedClient;
    }

    /**
     * Store a file in SeaweedFS and return its file ID (fid)
     * 
     * Process:
     * 1. Get assignment from master server
     * 2. Upload file to volume server
     * 3. Return the file ID
     */
    public String store(MultipartFile file) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file " + filename);
        }
        if (filename.contains("..")) {
            throw new IOException("Cannot store file with relative path outside current directory " + filename);
        }

        try {
            SeaweedFsClient.FileAssignment assignment = seaweedClient.assignFile();
            seaweedClient.uploadFile(assignment.publicUrl(), assignment.fid(), filename, file.getBytes());
            return assignment.fid();
        } catch (Exception e) {
            throw new IOException("Error uploading file to SeaweedFS: " + e.getMessage(), e);
        }
    }

    /**
     * Compatibility helper retained for existing service/integration tests.
     */
    public Path load(String filename) {
        return Path.of(filename);
    }

    /**
     * Load and return file as a Resource for download
     */
    public Resource loadAsResource(String fileId) throws IOException {
        if (fileId == null || fileId.isEmpty()) {
            throw new IOException("Invalid file ID");
        }

        try {
            return new ByteArrayResource(seaweedClient.downloadFile(fileId));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not read file: " + fileId, e);
        }
    }

    /**
     * Delete a file from SeaweedFS
     */
    public void delete(String fileId) throws IOException {
        if (fileId == null || fileId.isEmpty()) {
            throw new IOException("Invalid file ID");
        }

        try {
            seaweedClient.deleteFile(fileId);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete file from SeaweedFS: " + fileId);
            // Don't fail hard—file might already be deleted
        }
    }
}
