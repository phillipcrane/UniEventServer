package dk.unievent.web.controller;

import dk.unievent.web.dto.PageDTO;
import dk.unievent.web.service.PageService;
import dk.unievent.web.model.MediaEntity;
import dk.unievent.web.service.MediaService;
import dk.unievent.web.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * REST Controller for Page endpoints (Organizers/Facebook Pages)
 * All endpoints are prefixed with /api/pages
 * 
 * Example requests:
 * GET  /api/pages              - Get all pages
 * GET  /api/pages/active       - Get only active pages
 * GET  /api/pages/{id}         - Get page by ID
 * POST /api/pages              - Create new page (called by Facebook sync)
 * PUT  /api/pages/{id}         - Update page info
 * DELETE /api/pages/{id}       - Delete page and all events
 */
@RestController
@RequestMapping("/api/pages")
@Tag(name = "Pages", description = "Manage event organizer pages (Facebook pages)")
public class PageController {
    
    @Autowired
    private PageService pageService;
    
    @Autowired
    private MediaService mediaService;
    
    @Autowired
    private MediaRepository mediaRepository;
    
    /**
     * GET /api/pages
     * Return ALL pages (organizers) ordered by name
     * 
     * This is called by the frontend to show the list of "filters by organizer"
     * Example response:
     * [
     *   { id: "123", name: "S-huset", url: "https://facebook.com/123", active: true, pictureUrl: "..." },
     *   { id: "456", name: "Pumpehuset", url: "https://facebook.com/456", active: true, pictureUrl: "..." }
     * ]
     */
    @GetMapping
    @Operation(summary = "Get all pages", description = "Retrieve all event organizer pages ordered by name")
    @ApiResponse(responseCode = "200", description = "List of pages")
    public ResponseEntity<List<PageDTO>> getAllPages() {
        List<PageDTO> pages = pageService.getAllPages();
        return ResponseEntity.ok(pages);
    }
    
    /**
     * GET /api/pages/active
     * Return ONLY ACTIVE pages (tokenStatus = "valid")
     * 
     * Active = the page's Facebook token is still valid and working
     * Only these pages will have up-to-date events in the system
     * 
     * Useful for: Show which organizers we're currently syncing from
     * Example response: Smaller list of PageDTO with active: true
     */
    @GetMapping("/active")
    @Operation(summary = "Get active pages", description = "Retrieve only pages with valid Facebook tokens")
    @ApiResponse(responseCode = "200", description = "List of active pages")
    public ResponseEntity<List<PageDTO>> getActivePages() {
        List<PageDTO> pages = pageService.getActivePages();
        return ResponseEntity.ok(pages);
    }
    
    /**
     * GET /api/pages/{id}
     * Get a SINGLE page by ID
     * 
     * Parameter: id = Facebook page ID
     * Example: GET /api/pages/123456789
     * Returns: Single PageDTO with public info (no tokens or internal tracking)
     * 
     * Returns 404 if page doesn't exist
     */
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
    
    /**
     * GET /api/pages/search
     * SEARCH for pages by name (case-insensitive)
     * 
     * Parameter: name (query param) = partial name to search
     * Example: GET /api/pages/search?name=s-huset
     * Returns: List of PageDTO matching that name
     * 
     * Useful for: Autocomplete/search feature
     */
    @GetMapping("/search")
    @Operation(summary = "Search pages by name", description = "Search for pages using a partial name match (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "List of matching pages")
    public ResponseEntity<List<PageDTO>> searchPages(
            @RequestParam(name = "name") @Parameter(description = "Partial page name to search for") String name) {
        List<PageDTO> pages = pageService.searchPagesByName(name);
        return ResponseEntity.ok(pages);
    }
    
    /**
     * POST /api/pages
     * CREATE a new page (organizer)
     * 
     * This is called by the Facebook OAuth callback handler
     * When a user authorizes the app and selects their pages
     * 
     * Request body: PageDTO with page info
     * Example:
     * {
     *   "id": "123456789",
     *   "name": "S-huset",
     *   "pictureUrl": "https://..."
     * }
     * 
     * Returns: Created PageDTO (HTTP 201 Created)
     * Note: Internal fields (tokens, refresh status) are NOT in the response
     */
    @PostMapping
    @Operation(summary = "Create a new page", description = "Create a new event organizer page")
    @ApiResponse(responseCode = "201", description = "Page created successfully")
    public ResponseEntity<PageDTO> createPage(@Valid @RequestBody PageDTO pageDTO) {
        PageDTO created = pageService.savePage(pageDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * PUT /api/pages/{id}
     * UPDATE page information
     * 
     * Parameter: id = page ID to update
     * Request body: PageDTO with new values
     * Example: PUT /api/pages/123456789
     * Body: { "name": "New Name", "pictureUrl": "https://..." }
     * 
     * Returns: Updated PageDTO
     * Returns: HTTP 200 OK, or 404 if not found
     * 
     * Use case: Update page name/picture when Facebook page changes
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a page", description = "Update page information")
    @ApiResponse(responseCode = "200", description = "Page updated successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> updatePage(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @Valid @RequestBody PageDTO pageDTO) {
        pageDTO.setId(id);  // Ensure we're updating the right page
        PageDTO updated = pageService.savePage(pageDTO);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/pages/{id}/picture
     * Upload a picture for a page
     * 
     * Parameter: id = page ID
     * File: multipart file (image file)
     * Example: POST /api/pages/123456789/picture with image file
     * 
     * Returns: Updated PageDTO with pictureId set
     * Returns: HTTP 200 OK
     */
    @PostMapping("/{id}/picture")
    @Operation(summary = "Upload picture for page", description = "Upload a picture for a specific page")
    @ApiResponse(responseCode = "200", description = "Picture uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Page not found")
    public ResponseEntity<PageDTO> uploadPagePicture(
            @PathVariable @Parameter(description = "Facebook page ID") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Verify page exists
        PageDTO pageDTO = pageService.getPageById(id);
        if (pageDTO == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Store the file
        String storedFilename = mediaService.store(file);
        MediaEntity mediaEntity = new MediaEntity(file.getOriginalFilename(), file.getContentType(), storedFilename);
        MediaEntity saved = mediaRepository.save(mediaEntity);
        
        // Update page with picture
        pageDTO.setPictureId(saved.getId());
        PageDTO updated = pageService.savePage(pageDTO);
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * DELETE /api/pages/{id}
     * DELETE a page (organizer) and ALL its events
     * 
     * Parameter: id = page ID to delete
     * Example: DELETE /api/pages/123456789
     * 
     * This is a DESTRUCTIVE operation:
     * - Deletes the page
     * - Deletes all events from this page (cascade delete)
     * - Deletes the stored Facebook token
     * 
     * Returns: HTTP 204 No Content (success)
     *          HTTP 404 Not Found (page doesn't exist)
     */
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
    
    /**
     * Internal endpoint (not exposed to frontend)
     * GET /api/pages/admin/to-refresh
     * Get pages that need token refresh (for the scheduled job)
     * 
     * This is called by: Backend scheduled task every 45 days
     * Used for: Token refresh service to know which pages to update
     * 
     * Returns: List of PageEntity (not DTO) with token info
     * NOTE: This endpoint would need @GetMapping("/admin/to-refresh") and admin auth
     */
}
