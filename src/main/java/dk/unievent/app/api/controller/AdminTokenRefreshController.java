package dk.unievent.app.api.controller;

import dk.unievent.app.api.dto.AdminRefreshResult;
import dk.unievent.app.api.dto.AdminRefreshSummary;
import dk.unievent.app.application.dto.TokenRefreshResult;
import dk.unievent.app.application.service.FacebookTokenRefreshService;
import dk.unievent.app.db.repository.PageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual admin trigger for the same Facebook page-token refresh logic the scheduler runs.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tools/refresh-tokens")
@Tag(name = "Admin Tools - Token Refresh", description = "Manually refresh Facebook page tokens")
public class AdminTokenRefreshController {

    private final FacebookTokenRefreshService tokenRefreshService;
    private final PageRepository pageRepository;

    public AdminTokenRefreshController(FacebookTokenRefreshService tokenRefreshService, PageRepository pageRepository) {
        this.tokenRefreshService = tokenRefreshService;
        this.pageRepository = pageRepository;
    }

    @PostMapping
    @Operation(summary = "Refresh all page tokens", description = "Refreshes tokens for every page regardless of expiry.")
    public ResponseEntity<AdminRefreshSummary> refreshAll() {
        log.info("Received refresh-all-tokens request");
        return ResponseEntity.ok(AdminRefreshSummary.from(tokenRefreshService.refreshAllForce()));
    }

    @PostMapping("/{pageId}")
    @Operation(summary = "Refresh one page's token", description = "Vault read -> Graph API refresh -> Vault write -> DB metadata update, scoped to a single page.")
    public ResponseEntity<AdminRefreshResult> refreshOne(@PathVariable String pageId) {
        log.info("Received refresh-token request for page: {}", pageId);
        if (!pageRepository.existsById(pageId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AdminRefreshResult(pageId, false, "Page not found"));
        }
        TokenRefreshResult result = tokenRefreshService.refreshOne(pageId);
        AdminRefreshResult response = AdminRefreshResult.from(result);
        return result.success()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
