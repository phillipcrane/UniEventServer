package dk.unievent.app.application.service;

import dk.unievent.app.infrastructure.client.SeaweedFsClient;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;

@Slf4j
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
        log.info("Storing file: {}, size: {} bytes", filename, file.getSize());
        if (file.isEmpty()) {
            log.warn("Cannot store empty file: {}", filename);
            throw new IllegalArgumentException("Failed to store empty file");
        }
        if (filename.contains("..")) {
            log.warn("Cannot store file with relative path: {}", filename);
            throw new IllegalArgumentException("Cannot store file with relative path outside current directory");
        }

        try {
            SeaweedFsClient.FileAssignment assignment = seaweedClient.assignFile();
            seaweedClient.uploadFile(assignment.publicUrl(), assignment.fid(), filename, file.getBytes());
            log.info("File stored successfully with id: {}", assignment.fid());
            return assignment.fid();
        } catch (Exception e) {
            log.error("Error uploading file to SeaweedFS: {}", filename, e);
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
            log.warn("Invalid file ID for download");
            throw new IOException("Invalid file ID");
        }

        log.debug("Loading file as resource: {}", fileId);
        try {
            byte[] data = seaweedClient.downloadFile(fileId);
            log.debug("File downloaded successfully: {}", fileId);
            return new ByteArrayResource(data);
        } catch (IOException e) {
            log.error("I/O error reading file: {}", fileId, e);
            throw e;
        } catch (Exception e) {
            log.error("Could not read file: {}", fileId, e);
            throw new IOException("Could not read file: " + fileId, e);
        }
    }

    /**
     * Delete a file from SeaweedFS
     */
    public void delete(String fileId) throws IOException {
        if (fileId == null || fileId.isEmpty()) {
            log.warn("Invalid file ID for deletion");
            throw new IOException("Invalid file ID");
        }

        log.debug("Deleting file: {}", fileId);
        try {
            seaweedClient.deleteFile(fileId);
            log.info("File deleted successfully: {}", fileId);
        } catch (Exception e) {
            log.warn("Could not delete file from SeaweedFS: {}", fileId, e);
            // Don't fail hard-file might already be deleted
        }
    }

    /**
     * Download an image from a URL and store it in SeaweedFS.
     * Used for Facebook event cover images and other remote media.
     * @param imageUrl URL of the image to download
     * @param filename Filename for storage
     * @return File ID (fid) of the stored image
     * @throws IOException if download or upload fails
     */
    public String downloadAndStoreImage(String imageUrl, String filename) throws IOException {
        log.debug("Downloading image from URL: {}", imageUrl);

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }

        if (filename == null || filename.isEmpty()) {
            filename = "image_" + System.currentTimeMillis() + ".jpg";
        }

        filename = StringUtils.cleanPath(filename);
        if (filename.contains("..")) {
            throw new IllegalArgumentException("Cannot store file with relative path outside current directory");
        }

        try {
            // Step 1: Download image from URL
            log.debug("Downloading image from URL: {}", imageUrl);
            byte[] imageData = downloadImageBytes(imageUrl);
            log.debug("Downloaded {} bytes from URL: {}", imageData.length, imageUrl);

            // Step 2: Get assignment from SeaweedFS master server
            SeaweedFsClient.FileAssignment assignment = seaweedClient.assignFile();
            log.debug("File assignment obtained: {}", assignment.fid());

            // Step 3: Upload to SeaweedFS volume server
            seaweedClient.uploadFile(assignment.publicUrl(), assignment.fid(), filename, imageData);
            log.info("Image stored successfully in SeaweedFS with ID: {}", assignment.fid());

            return assignment.fid();

        } catch (IOException e) {
            log.error("IO error downloading/storing image from URL: {}", imageUrl, e);
            throw e;
        } catch (Exception e) {
            log.error("Error downloading/storing image from URL: {}", imageUrl, e);
            throw new IOException("Error downloading/storing image: " + e.getMessage(), e);
        }
    }

    /**
     * Download image bytes from a URL with timeout.
     * @param imageUrl URL to download from
     * @return Byte array of image data
     * @throws IOException if download fails
     */
    private byte[] downloadImageBytes(String imageUrl) throws IOException {
        try {
            var url = URI.create(imageUrl).toURL();
            URLConnection conn = url.openConnection();
            
            // Set timeout to 10 seconds
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            // Set user agent to avoid blocking by some servers
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; UniEventServer/1.0; +http://unievent.dk)");

            try (InputStream in = conn.getInputStream()) {
                byte[] imageData = in.readAllBytes();
                log.debug("Downloaded {} bytes from URL", imageData.length);
                return imageData;
            }
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
}
