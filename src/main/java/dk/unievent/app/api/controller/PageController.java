package dk.unievent.app.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.service.PageService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
@Tag(name = "Pages", description = "Manage event organizer pages (Facebook pages)")
public class PageController {

    private final PageService pageService;

    @GetMapping
    @Operation(summary = "Get all pages", description = "Retrieve all event organizer pages ordered by name")
    @ApiResponse(responseCode = "200", description = "Page of pages")
    public ResponseEntity<Page<PageDTO>> getAllPages(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching all pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> pages = pageService.getAllPages(pageable);
        log.debug("Retrieved {} pages", pages.getTotalElements());
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active pages", description = "Retrieve only pages with valid Facebook tokens")
    @ApiResponse(responseCode = "200", description = "Page of active pages")
    public ResponseEntity<Page<PageDTO>> getActivePages(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching active pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> pages = pageService.getActivePages(pageable);
        log.debug("Retrieved {} active pages", pages.getTotalElements());
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get page by ID", description = "Retrieve a single page by its Facebook ID")
    @ApiResponse(responseCode = "200", description = "Page found")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> getPageById(@PathVariable @Parameter(description = "Facebook page ID") String id) {
        log.debug("Fetching page with id: {}", id);
        Optional<PageDTO> page = pageService.getPageById(id);
        if (page.isEmpty()) {
            log.warn("Page not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.debug("Page found: {}", id);
        return ResponseEntity.ok(page.get());
    }

    @GetMapping("/search")
    @RateLimiter(name = "page-search", fallbackMethod = "searchFallback")
    @Operation(summary = "Search pages by name", description = "Search for pages using a partial name match (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Page of matching pages")
    public ResponseEntity<Page<PageDTO>> searchPages(
            @RequestParam(name = "name") @Parameter(description = "Partial page name to search for") String name,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching pages by name: {}", name);
        Page<PageDTO> pages = pageService.searchPagesByName(name, pageable);
        log.debug("Found {} pages matching: {}", pages.getTotalElements(), name);
        return ResponseEntity.ok(pages);
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "page-create", fallbackMethod = "createFallback")
    @Operation(summary = "Create a new page", description = "Create a new event organizer page")
    @ApiResponse(responseCode = "201", description = "Page created successfully")
    public ResponseEntity<PageDTO> createPage(@Valid @RequestBody PageDTO pageDTO) {
        log.info("Creating new page: {}", pageDTO.getName());
        PageDTO created = pageService.savePage(pageDTO);
        log.info("Page created successfully with id: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "page-update", fallbackMethod = "updateFallback")
    @Operation(summary = "Update a page", description = "Update page information")
    @ApiResponse(responseCode = "200", description = "Page updated successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> updatePage(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @Valid @RequestBody PageDTO pageDTO) {
        log.info("Updating page with id: {}", id);
        pageDTO.setId(id);
        Optional<PageDTO> updated = pageService.updatePage(id, pageDTO);
        if (updated.isEmpty()) {
            log.warn("Page not found for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Page updated successfully: {}", id);
        return ResponseEntity.ok(updated.get());
    }

    @PostMapping("/{id}/picture")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Upload picture for page", description = "Upload a picture for a specific page")
    @ApiResponse(responseCode = "200", description = "Picture uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> uploadPagePicture(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Uploading picture for page id: {}, filename: {}", id, file.getOriginalFilename());
        Optional<PageDTO> updated = pageService.uploadPicture(id, file);
        if (updated.isEmpty()) {
            log.warn("Page not found for picture upload with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Picture uploaded successfully for page: {}", id);
        return ResponseEntity.ok(updated.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "page-delete", fallbackMethod = "deleteFallback")
    @Operation(summary = "Delete a page", description = "Delete a page and all its events (cascading delete)")
    @ApiResponse(responseCode = "204", description = "Page deleted successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<Void> deletePage(@PathVariable @Parameter(description = "Facebook page ID") String id) {
        log.info("Deleting page with id: {}", id);
        boolean deleted = pageService.deletePage(id);
        if (!deleted) {
            log.warn("Page not found for deletion with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Page deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Page<PageDTO>> searchFallback(String name, Pageable pageable, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<PageDTO> createFallback(PageDTO pageDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<PageDTO> updateFallback(String id, PageDTO pageDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<Void> deleteFallback(String id, Exception ex) {
        return ResponseEntity.status(429).build();
    }
}
