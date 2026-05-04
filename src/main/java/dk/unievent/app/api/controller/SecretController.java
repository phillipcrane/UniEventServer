package dk.unievent.app.api.controller;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only endpoint for inspecting the secrets registry (DB metadata about what lives in Vault).
 * Requires admin role - also enforced at the path level in SecurityConfig (/api/admin/**).
 * Only available when Vault is enabled (unievent.vault.enabled=true).
 */
@RestController
@RequestMapping("/api/admin/secrets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
@ConditionalOnBean(VaultService.class)
public class SecretController {

    private final VaultService vaultService;

    @GetMapping
    public List<SecretDTO> getAllSecrets() {
        return vaultService.getAllSecrets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecretDTO> getSecretById(@PathVariable Long id) {
        return vaultService.getSecretById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<SecretDTO> getSecretByName(@PathVariable String name) {
        return vaultService.getSecretByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-type/{type}")
    public Page<SecretDTO> getSecretsByType(@PathVariable String type, Pageable pageable) {
        return vaultService.getSecretsByType(type, pageable);
    }

    @GetMapping("/by-status/{status}")
    public Page<SecretDTO> getSecretsByStatus(@PathVariable String status, Pageable pageable) {
        return vaultService.getSecretsByStatus(status, pageable);
    }

    /**
     * Removes a record from the secrets registry. Does NOT delete the secret from Vault itself.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSecret(@PathVariable Long id) {
        return vaultService.deleteSecret(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
