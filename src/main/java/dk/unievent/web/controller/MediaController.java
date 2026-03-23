package dk.unievent.web.controller;

import dk.unievent.web.model.MediaEntity;
import dk.unievent.web.repository.MediaRepository;
import dk.unievent.web.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;
    private final MediaRepository repository;

    public MediaController(MediaService mediaService, MediaRepository repository) {
        this.mediaService = mediaService;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<MediaEntity> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String storedFilename = mediaService.store(file);
        MediaEntity meta = new MediaEntity(file.getOriginalFilename(), file.getContentType(), storedFilename);
        repository.save(meta);
        return ResponseEntity.ok(meta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        MediaEntity media = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found: " + id));
        // path stored in DB is the filename relative to storage root
        Resource resource = mediaService.loadAsResource(media.getPath());
        String encoded = URLEncoder.encode(media.getFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encoded + "\"")
                .body(resource);
    }

    @GetMapping
    public List<MediaEntity> list() {
        return repository.findAll();
    }
}
