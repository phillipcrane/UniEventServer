package dk.unievent.app.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.unievent.app.infrastructure.config.VaultConfig;

@ExtendWith(MockitoExtension.class)
class VaultClientTests {

    @Mock
    private RestTemplate restTemplate;

    private VaultClient vaultClient;

    @BeforeEach
    void setUp() {
        VaultConfig config = new VaultConfig();
        config.setUri("http://localhost:8200");
        config.setSecretPath("secret/data/unievent");
        config.setToken("dev-token");
        vaultClient = new VaultClient(config, restTemplate, new ObjectMapper());
    }

    @Test
    void readSecretDataShouldReturnEmptyMapWhenVaultStatusNotOk() {
        when(restTemplate.exchange(
                eq("http://localhost:8200/v1/secret/data/unievent"),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden"));

        Map<String, String> result = vaultClient.readSecretData();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void readSecretDataShouldReturnParsedValuesWhenResponseValid() {
        when(restTemplate.exchange(
                eq("http://localhost:8200/v1/secret/data/unievent"),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"data\":{\"data\":{\"DB_USER\":\"unievent\",\"DB_PASS\":\"secret\"}}}"));

        Map<String, String> result = vaultClient.readSecretData();

        assertEquals("unievent", result.get("DB_USER"));
        assertEquals("secret", result.get("DB_PASS"));
        assertEquals(2, result.size());
    }

    @Test
    void readSecretValueShouldReturnNullWhenKeyNotPresent() {
        when(restTemplate.exchange(
                eq("http://localhost:8200/v1/secret/data/unievent"),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"data\":{\"data\":{\"OTHER\":\"value\"}}}"));

        String result = vaultClient.readSecretValue("MISSING");

        assertEquals(null, result);
    }

    @Test
    void readSecretDataShouldWrapUnexpectedFailures() {
        when(restTemplate.exchange(
                eq("http://localhost:8200/v1/secret/data/unievent"),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)))
            .thenThrow(new RuntimeException("network down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vaultClient.readSecretData());
        assertTrue(ex.getMessage().contains("Failed to read secrets from Vault"));
    }
}
