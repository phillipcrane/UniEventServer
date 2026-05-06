package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.MediaDTO;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.infrastructure.client.SeaweedFsClient;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// stores and retrieves media files in SeaweedFS. Handles upload from multipart forms and
// download-and-store from remote URLs (Facebook CDN cover images). MIME type is detected from
// file bytes rather than trusting the client, and remote downloads are restricted to an allowlist.
@Slf4j
@Service
public class MediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> FACEBOOK_CDN_HOSTS = Set.of("fbcdn.net", "facebook.com", "cdninstagram.com", "instagram.com");
    private static final int MAX_REMOTE_IMAGE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Value("${unievent.media.ssrf.extra-allowed-hosts:}")
    private String extraAllowedHostsConfig;

    private Set<String> allowedImageHosts;

    private final SeaweedFsClient seaweedClient;
    private final MediaRepository mediaRepository;

    public MediaService(SeaweedFsClient seaweedClient, MediaRepository mediaRepository) {
        this.seaweedClient = seaweedClient;
        this.mediaRepository = mediaRepository;
    }

    public Page<MediaDTO> listAll(Pageable pageable) {
        return mediaRepository.findAll(pageable).map(this::toDto);
    }

    public MediaEntity findById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Media not found with id: {}", id);
                    return new java.util.NoSuchElementException("Media not found: " + id);
                });
    }

    public MediaDTO storeAndSave(MultipartFile file) throws IOException {
        String fid = store(file);
        String detectedContentType = detectMimeType(file.getBytes());
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        MediaEntity entity = MediaEntity.builder()
                .filename(sanitizedFilename)
                .contentType(detectedContentType)
                .fileId(fid)
                .uploadedAt(Instant.now())
                .build();
        return toDto(mediaRepository.save(entity));
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "upload_" + System.currentTimeMillis();
        }
        return Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private MediaDTO toDto(MediaEntity entity) {
        return MediaDTO.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .contentType(entity.getContentType())
                .fileId(entity.getFileId())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    @PostConstruct
    void buildAllowlist() {
        allowedImageHosts = new HashSet<>(FACEBOOK_CDN_HOSTS);
        if (extraAllowedHostsConfig != null && !extraAllowedHostsConfig.isBlank()) {
            Set<String> extra = Arrays.stream(extraAllowedHostsConfig.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            allowedImageHosts.addAll(extra);
            log.info("SSRF allowlist extended with dev hosts: {}", extra);
        }
    }

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
            byte[] bytes = file.getBytes();
            String detectedType = detectMimeType(bytes);
            if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
                log.warn("Rejected upload with unsupported MIME type '{}': {}", detectedType, filename);
                throw new IllegalArgumentException(
                    "Unsupported file type '" + detectedType + "'. Allowed: image/jpeg, image/png, image/webp");
            }
            SeaweedFsClient.FileAssignment assignment = seaweedClient.assignFile();
            seaweedClient.uploadFile(assignment.publicUrl(), assignment.fid(), filename, bytes);
            log.info("File stored successfully with id: {}", assignment.fid());
            return assignment.fid();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading file to SeaweedFS: {}", filename, e);
            throw new IOException("Error uploading file to SeaweedFS: " + e.getMessage(), e);
        }
    }

    // kept for compatibility with existing service/integration tests
    public Path load(String filename) {
        return Path.of(filename);
    }

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
            log.warn("Could not delete file from SeaweedFS: {}", fileId, e); // swallow, file may already be gone
        }
    }

    // downloads a cover image from a remote URL and stores it in SeaweedFS. used for Facebook event covers.
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
            // 1. download image bytes (SSRF checks, HTTPS-only, allowlist, and 10 MB cap are in downloadImageBytes)
            byte[] imageData = downloadImageBytes(imageUrl);
            log.debug("Downloaded {} bytes from URL: {}", imageData.length, imageUrl);

            // 2. get a slot from the SeaweedFS master, then upload to the assigned volume
            SeaweedFsClient.FileAssignment assignment = seaweedClient.assignFile();
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

    private String detectMimeType(byte[] bytes) {
        // Check WebP: RIFF????WEBP
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            String type = URLConnection.guessContentTypeFromStream(is);
            return type != null ? type : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    // downloads image bytes from a URL. SSRF protection: HTTPS-only, host allowlist, 10 MB cap.
    private byte[] downloadImageBytes(String imageUrl) throws IOException {
        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid image URL: " + imageUrl);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Only HTTPS image URLs are permitted, got: " + uri.getScheme());
        }
        String host = uri.getHost();
        if (host == null) {
            throw new IOException("Image URL has no host: " + imageUrl);
        }
        boolean hostAllowed = allowedImageHosts.stream()
                .anyMatch(h -> host.equals(h) || host.endsWith("." + h));
        if (!hostAllowed) {
            throw new IOException("Image URL host not in allowlist: " + host);
        }

        try {
            URLConnection conn = uri.toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; UniEventServer/1.0; +http://unievent.dk)");

            try (InputStream in = conn.getInputStream()) {
                String contentType = conn.getContentType();
                if (contentType != null && !contentType.startsWith("image/")) {
                    throw new IOException("Unexpected content-type from remote URL: " + contentType);
                }
                byte[] imageData = in.readNBytes(MAX_REMOTE_IMAGE_BYTES + 1);
                if (imageData.length > MAX_REMOTE_IMAGE_BYTES) {
                    throw new IOException("Remote image exceeds maximum allowed size of " + MAX_REMOTE_IMAGE_BYTES + " bytes");
                }
                log.debug("Downloaded {} bytes from URL", imageData.length);
                return imageData;
            }
        } catch (IOException e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
}
