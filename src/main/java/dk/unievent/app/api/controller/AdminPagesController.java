package dk.unievent.app.api.controller;

import dk.unievent.app.api.dto.AdminPageSummary;
import dk.unievent.app.db.repository.PageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lists Facebook pages tracked by the app for admin operators and the CLI picker.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tools/pages")
@Tag(name = "Admin Tools - Pages", description = "List tracked Facebook pages")
public class AdminPagesController {

    private final PageRepository pageRepository;

    public AdminPagesController(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @GetMapping
    @Operation(summary = "List all tracked pages", description = "Returns id, name, token status, and days-to-expiry for every page.")
    public ResponseEntity<List<AdminPageSummary>> list() {
        log.info("Received list-pages request");
        List<AdminPageSummary> summaries = pageRepository
            .findAllByOrderByNameAsc(PageRequest.of(0, 500))
            .map(AdminPageSummary::from)
            .getContent()
            .stream()
            .toList();
        return ResponseEntity.ok(summaries);
    }
}
