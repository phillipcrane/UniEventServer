package dk.unievent.storage;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
/**
 * Handles image storage operations backed by Google Cloud Storage.
 */
public class StorageController {

    private final Bucket bucket;

    public StorageController(Storage storage, @Value("${gcp.storage.bucket:unievent-images}") String bucketName) {
        this.bucket = storage.get(bucketName);
    }

    /**
     * Stores raw image bytes at the provided object path.
     */
    @PostMapping("/images")
    public ResponseEntity<Map<String, Object>> addImage(
            @RequestParam String filePath,
            @RequestParam String contentType,
            @RequestBody byte[] data) {
        try {
            Blob blob = bucket.create(filePath, data, contentType);
            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
            String mediaLink = blob.getMediaLink();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "mediaLink", mediaLink,
                "filePath", filePath
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Stores a multipart file upload at the provided object path.
     */
    @PostMapping("/images/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam String filePath) {
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            byte[] data = file.getBytes();

            Blob blob = bucket.create(filePath, data, contentType);
            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
            String mediaLink = blob.getMediaLink();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "mediaLink", mediaLink,
                "filePath", filePath,
                "size", data.length
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Placeholder endpoint for URL-based ingestion.
     * Current implementation validates request shape and returns an acknowledgement.
     */
    @PostMapping("/images/from-url")
    public ResponseEntity<Map<String, Object>> addImageFromUrl(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            String sourceUrl = request.get("sourceUrl");

            if (filePath == null || sourceUrl == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing filePath or sourceUrl parameters"));
            }

            // For now, return a placeholder response since URL downloading requires additional implementation
            // In a real implementation, you'd download the image from the URL
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Image URL processing not fully implemented",
                "filePath", filePath,
                "sourceUrl", sourceUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns metadata for a stored image object.
     */
    @GetMapping("/images/{filePath}")
    public ResponseEntity<?> getImage(@PathVariable String filePath) {
        try {
            Blob blob = bucket.get(filePath);
            if (blob == null || !blob.exists()) {
                return ResponseEntity.notFound().build();
            }

            String mediaLink = blob.getMediaLink();
            return ResponseEntity.ok(Map.of(
                "mediaLink", mediaLink,
                "filePath", filePath,
                "exists", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Deletes an image object from the configured bucket.
     */
    @DeleteMapping("/images/{filePath}")
    public ResponseEntity<Map<String, Object>> removeImage(@PathVariable String filePath) {
        try {
            bucket.get(filePath).delete();
            return ResponseEntity.ok(Map.of("success", true, "message", "Image deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}