package dk.unievent.app.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VaultPropertiesTests {

    @Test
    void testDefaults() {
        VaultProperties props = new VaultProperties();
        assertFalse(props.isEnabled());
        assertEquals("http://localhost:8200", props.getUri());
        assertEquals("", props.getToken());
        assertEquals("secret/data/unievent", props.getSecretPath());
    }

    @Test
    void testSetters() {
        VaultProperties props = new VaultProperties();
        props.setEnabled(true);
        props.setUri("http://vault:8200");
        props.setToken("s.mytoken");
        props.setSecretPath("secret/data/myapp");

        assertTrue(props.isEnabled());
        assertEquals("http://vault:8200", props.getUri());
        assertEquals("s.mytoken", props.getToken());
        assertEquals("secret/data/myapp", props.getSecretPath());
    }
}
