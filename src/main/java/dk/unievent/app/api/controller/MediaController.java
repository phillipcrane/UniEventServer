package dk.unievent.app.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.MediaDTO;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.application.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "APIs for uploading, downloading, and managing media files")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping
    @Operation(
        summary = "Upload a media file",
        description = "Upload a file to SeaweedFS and store metadata in the database. Returns the created media record with ID and file ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
        @ApiResponse(responseCode = "500", description = "Server error during file storage")
    })
    public ResponseEntity<MediaDTO> upload(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Uploading media file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        MediaDTO saved = mediaService.storeAndSave(file);
        log.info("Media file uploaded successfully with id: {}", saved.getId());
        return ResponseEntity.ok(saved);
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
        log.debug("Fetching media with id: {}", id);
        MediaEntity media = mediaService.findById(id);
        log.debug("Media found: {}", id);
        Resource resource = mediaService.loadAsResource(media.getFileId());
        String encoded = URLEncoder.encode(media.getFilename(), StandardCharsets.UTF_8);
        MediaType contentType = media.getContentType() != null
                ? MediaType.parseMediaType(media.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encoded + "\"")
                .body(resource);
    }

    @GetMapping
    @Operation(
        summary = "List all media files",
        description = "Retrieve a list of all uploaded media files with their metadata."
    )
    @ApiResponse(responseCode = "200", description = "List of media files",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDTO.class)))
    public Page<MediaDTO> list(@PageableDefault(size = 50) Pageable pageable) {
        log.debug("Listing media files: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<MediaDTO> media = mediaService.listAll(pageable);
        log.debug("Found {} media files", media.getTotalElements());
        return media;
    }

}
