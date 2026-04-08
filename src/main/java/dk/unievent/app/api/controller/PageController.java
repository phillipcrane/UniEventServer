package dk.unievent.app.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.service.PageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/pages")
@Tag(name = "Pages", description = "Manage event organizer pages (Facebook pages)")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    @Operation(summary = "Get all pages", description = "Retrieve all event organizer pages ordered by name")
    @ApiResponse(responseCode = "200", description = "List of pages")
    public ResponseEntity<List<PageDTO>> getAllPages() {
        List<PageDTO> pages = pageService.getAllPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active pages", description = "Retrieve only pages with valid Facebook tokens")
    @ApiResponse(responseCode = "200", description = "List of active pages")
    public ResponseEntity<List<PageDTO>> getActivePages() {
        List<PageDTO> pages = pageService.getActivePages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get page by ID", description = "Retrieve a single page by its Facebook ID")
    @ApiResponse(responseCode = "200", description = "Page found")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> getPageById(@PathVariable @Parameter(description = "Facebook page ID") String id) {
        PageDTO page = pageService.getPageById(id);

        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    @Operation(summary = "Search pages by name", description = "Search for pages using a partial name match (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "List of matching pages")
    public ResponseEntity<List<PageDTO>> searchPages(
            @RequestParam(name = "name") @Parameter(description = "Partial page name to search for") String name) {
        List<PageDTO> pages = pageService.searchPagesByName(name);
        return ResponseEntity.ok(pages);
    }

    @PostMapping
    @Operation(summary = "Create a new page", description = "Create a new event organizer page")
    @ApiResponse(responseCode = "201", description = "Page created successfully")
    public ResponseEntity<PageDTO> createPage(@Valid @RequestBody PageDTO pageDTO) {
        PageDTO created = pageService.savePage(pageDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a page", description = "Update page information")
    @ApiResponse(responseCode = "200", description = "Page updated successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> updatePage(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @Valid @RequestBody PageDTO pageDTO) {
        pageDTO.setId(id);
        PageDTO updated = pageService.savePage(pageDTO);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/picture")
    @Operation(summary = "Upload picture for page", description = "Upload a picture for a specific page")
    @ApiResponse(responseCode = "200", description = "Picture uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> uploadPagePicture(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        PageDTO updated = pageService.uploadPicture(id, file);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a page", description = "Delete a page and all its events (cascading delete)")
    @ApiResponse(responseCode = "204", description = "Page deleted successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<Void> deletePage(@PathVariable @Parameter(description = "Facebook page ID") String id) {
        boolean deleted = pageService.deletePage(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
