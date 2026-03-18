package dk.unievent.web.media;

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

    private final StorageService storageService;
    private final MediaFileRepository repository;

    public MediaController(StorageService storageService, MediaFileRepository repository) {
        this.storageService = storageService;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<MediaFile> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String storedFilename = storageService.store(file);
        MediaFile meta = new MediaFile(file.getOriginalFilename(), file.getContentType(), storedFilename);
        repository.save(meta);
        return ResponseEntity.ok(meta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        MediaFile media = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found: " + id));
        // path stored in DB is the filename relative to storage root
        Resource resource = storageService.loadAsResource(media.getPath());
        String encoded = URLEncoder.encode(media.getFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .body(resource);
    }

    @GetMapping
    public List<MediaFile> list() {
        return repository.findAll();
    }
}
