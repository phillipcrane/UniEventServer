package dk.unievent.app.tools.controller;

import dk.unievent.app.application.service.TokenRefreshService;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.tools.models.RefreshResult;
import dk.unievent.app.tools.models.RefreshSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual trigger for the same Facebook page-token refresh logic the
 * FacebookTokenRefresher scheduler runs every 20 days.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tools/refresh-tokens")
@Tag(name = "Admin Tools - Token Refresh", description = "Manually refresh Facebook page tokens")
@PreAuthorize("hasRole('admin')")
public class TokenController {

    private final TokenRefreshService tokenRefreshService;
    private final PageRepository pageRepository;

    public TokenController(TokenRefreshService tokenRefreshService, PageRepository pageRepository) {
        this.tokenRefreshService = tokenRefreshService;
        this.pageRepository = pageRepository;
    }

    @PostMapping
    @Operation(summary = "Refresh all page tokens", description = "Refreshes tokens for every page regardless of expiry.")
    public ResponseEntity<RefreshSummary> refreshAll() {
        log.info("Received refresh-all-tokens request");
        return ResponseEntity.ok(tokenRefreshService.refreshAllForce());
    }

    @PostMapping("/{pageId}")
    @Operation(summary = "Refresh one page's token", description = "Vault read → Graph API refresh → Vault write → DB metadata update, scoped to a single page.")
    public ResponseEntity<RefreshResult> refreshOne(@PathVariable String pageId) {
        log.info("Received refresh-token request for page: {}", pageId);
        if (!pageRepository.existsById(pageId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RefreshResult(pageId, false, "Page not found"));
        }
        RefreshResult result = tokenRefreshService.refreshOne(pageId);
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
