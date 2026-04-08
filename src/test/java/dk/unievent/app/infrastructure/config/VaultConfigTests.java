package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaultConfigTests {

    @Test
    void gettersAndSettersShouldStoreVaultProperties() {
        VaultConfig config = new VaultConfig();

        config.setEnabled(true);
        config.setUri("http://vault.internal:8200");
        config.setToken("root-token");
        config.setSecretPath("secret/data/prod");

        assertEquals(true, config.isEnabled());
        assertEquals("http://vault.internal:8200", config.getUri());
        assertEquals("root-token", config.getToken());
        assertEquals("secret/data/prod", config.getSecretPath());
    }
}
