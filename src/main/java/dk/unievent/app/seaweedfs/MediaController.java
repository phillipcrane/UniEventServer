package dk.unievent.app.seaweedfs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.mysql.model.MediaEntity;
import dk.unievent.app.mysql.repository.MediaRepository;
import dk.unievent.app.seaweedfs.MediaService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/media")
@Tag(name = "Media Management", description = "APIs for uploading, downloading, and managing media files")
public class MediaController {

    private final MediaService mediaService;
    private final MediaRepository repository;

    public MediaController(MediaService mediaService, MediaRepository repository) {
        this.mediaService = mediaService;
        this.repository = repository;
    }

    @PostMapping
    @Operation(
        summary = "Upload a media file",
        description = "Upload a file to SeaweedFS and store metadata in the database. Returns the created media record with ID and file ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaEntity.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
        @ApiResponse(responseCode = "500", description = "Server error during file storage")
    })
    public ResponseEntity<MediaEntity> upload(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") MultipartFile file) throws IOException {
        String storedFilename = mediaService.store(file);
        MediaEntity meta = MediaEntity.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileId(storedFilename)
                .uploadedAt(Instant.now())
                .build();
        repository.save(meta);
        return ResponseEntity.ok(meta);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Download a media file",
        description = "Download a media file by its ID. Retrieves the file from SeaweedFS and returns it with appropriate content type."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully",
            content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "404", description = "Media not found"),
        @ApiResponse(responseCode = "500", description = "Error retrieving file from storage")
    })
    public ResponseEntity<Resource> download(
        @Parameter(description = "Media file ID", required = true, example = "1")
        @PathVariable Long id) throws IOException {
        MediaEntity media = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found: " + id));
        // fileId stored in DB is the SeaweedFS file ID
        Resource resource = mediaService.loadAsResource(media.getFileId());
        String encoded = URLEncoder.encode(media.getFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encoded + "\"")
                .body(resource);
    }

    @GetMapping
    @Operation(
        summary = "List all media files",
        description = "Retrieve a list of all uploaded media files with their metadata."
    )
    @ApiResponse(responseCode = "200", description = "List of media files",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaEntity.class)))
    public List<MediaEntity> list() {
        return repository.findAll();
    }
}
