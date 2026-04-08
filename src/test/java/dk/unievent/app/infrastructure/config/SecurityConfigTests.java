package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTests {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthAndInfoShouldBePublic() throws Exception {
        HttpRequest health = HttpRequest.newBuilder().uri(URI.create(url("/actuator/health"))).GET().build();
        HttpRequest info = HttpRequest.newBuilder().uri(URI.create(url("/actuator/info"))).GET().build();

        HttpResponse<String> healthResponse = httpClient.send(health, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> infoResponse = httpClient.send(info, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, healthResponse.statusCode());
        assertEquals(200, infoResponse.statusCode());
    }

    @Test
    void otherActuatorEndpointsShouldBeDenied() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url("/actuator/metrics"))).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }
}
