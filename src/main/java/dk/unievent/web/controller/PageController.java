package dk.unievent.web.controller;

import dk.unievent.web.dto.PageDTO;
import dk.unievent.web.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
public class PageController {
    
    @Autowired
    private PageService pageService;
    
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
    public ResponseEntity<PageDTO> getPageById(@PathVariable String id) {
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
    public ResponseEntity<List<PageDTO>> searchPages(
            @RequestParam(name = "name") String name) {
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
    public ResponseEntity<PageDTO> createPage(@RequestBody PageDTO pageDTO) {
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
    public ResponseEntity<PageDTO> updatePage(
            @PathVariable String id,
            @RequestBody PageDTO pageDTO) {
        pageDTO.setId(id);  // Ensure we're updating the right page
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
    public ResponseEntity<Void> deletePage(@PathVariable String id) {
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
