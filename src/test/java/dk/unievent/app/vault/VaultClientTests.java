package dk.unievent.app.vault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import dk.unievent.app.vault.VaultClient;
import dk.unievent.app.vault.VaultProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VaultClientTests {

    private static final String VAULT_URI = "http://localhost:8200";
    private static final String SECRET_PATH = "secret/data/unievent";
    private static final String EXPECTED_URL = VAULT_URI + "/v1/" + SECRET_PATH;

    private VaultProperties properties;
    private MockRestServiceServer server;
    private VaultClient vaultClient;

    @BeforeEach
    void setUp() {
        properties = new VaultProperties();
        properties.setEnabled(true);
        properties.setUri(VAULT_URI);
        properties.setToken("test-token");
        properties.setSecretPath(SECRET_PATH);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        vaultClient = new VaultClient(properties, restTemplate);
    }

    @Test
    void testReadSecretsReturnsAllKeyValuePairs() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andExpect(header("X-Vault-Token", "test-token"))
              .andRespond(withSuccess("""
                      {
                        "data": {
                          "data": {
                            "FACEBOOK_CLIENT_ID": "abc123",
                            "FACEBOOK_CLIENT_SECRET": "secret456",
                            "SOME_API_KEY": "key789"
                          }
                        }
                      }
                      """, MediaType.APPLICATION_JSON));

        Map<String, String> secrets = vaultClient.readSecrets();

        assertEquals(3, secrets.size());
        assertEquals("abc123", secrets.get("FACEBOOK_CLIENT_ID"));
        assertEquals("secret456", secrets.get("FACEBOOK_CLIENT_SECRET"));
        assertEquals("key789", secrets.get("SOME_API_KEY"));
        server.verify();
    }

    @Test
    void testReadSecretReturnsSingleKey() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("""
                      {
                        "data": {
                          "data": {
                            "FACEBOOK_CLIENT_ID": "abc123",
                            "OTHER_KEY": "other"
                          }
                        }
                      }
                      """, MediaType.APPLICATION_JSON));

        String value = vaultClient.readSecret("FACEBOOK_CLIENT_ID");

        assertEquals("abc123", value);
    }

    @Test
    void testReadSecretReturnsNullForMissingKey() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("""
                      { "data": { "data": { "SOME_KEY": "value" } } }
                      """, MediaType.APPLICATION_JSON));

        String value = vaultClient.readSecret("NONEXISTENT_KEY");

        assertNull(value);
    }

    @Test
    void testReadSecretsReturnsEmptyMapWhenDataNodeMissing() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("""
                      { "request_id": "abc", "warnings": null }
                      """, MediaType.APPLICATION_JSON));

        Map<String, String> secrets = vaultClient.readSecrets();

        assertTrue(secrets.isEmpty());
    }

    @Test
    void testReadSecretsThrowsOnNonOkStatus() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withUnauthorizedRequest());

        assertThrows(RuntimeException.class, () -> vaultClient.readSecrets());
    }

    @Test
    void testReadSecretsThrowsOnServerError() {
        server.expect(requestTo(EXPECTED_URL))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> vaultClient.readSecrets());
    }

    @Test
    void testRequestUsesConfiguredToken() {
        properties.setToken("s.supersecrettoken");

        server.expect(requestTo(EXPECTED_URL))
              .andExpect(header("X-Vault-Token", "s.supersecrettoken"))
              .andRespond(withSuccess("""
                      { "data": { "data": {} } }
                      """, MediaType.APPLICATION_JSON));

        vaultClient.readSecrets();
        server.verify();
    }

    @Test
    void testRequestUsesConfiguredUriAndPath() {
        properties.setUri("http://vault:8200");
        properties.setSecretPath("secret/data/myapp");

        server.expect(requestTo("http://vault:8200/v1/secret/data/myapp"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("""
                      { "data": { "data": {} } }
                      """, MediaType.APPLICATION_JSON));

        vaultClient.readSecrets();
        server.verify();
    }
}
