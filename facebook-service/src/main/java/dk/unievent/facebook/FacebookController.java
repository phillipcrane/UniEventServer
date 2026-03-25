package dk.unievent.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facebook")
public class FacebookController {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${facebook.api.version:v23.0}")
    private String apiVersion;

    @Value("${facebook.app.id}")
    private String appId;

    @Value("${facebook.app.secret}")
    private String appSecret;

    @Value("${facebook.redirect.uri}")
    private String redirectUri;

    public FacebookController(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
            .baseUrl("https://graph.facebook.com/" + apiVersion)
            .build();
        this.objectMapper = objectMapper;
    }

    @PostMapping("/oauth/token")
    public ResponseEntity<Map<String, Object>> getShortLivedToken(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing code parameter"));
            }

            Map<String, String> params = Map.of(
                "client_id", appId,
                "redirect_uri", redirectUri,
                "client_secret", appSecret,
                "code", code
            );

            JsonNode response = restClient.get()
                .uri("/oauth/access_token?" + buildQueryString(params))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            String token = response.get("access_token").asText();
            return ResponseEntity.ok(Map.of("access_token", token));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/oauth/long-lived-token")
    public ResponseEntity<Map<String, Object>> getLongLivedToken(@RequestBody Map<String, String> request) {
        try {
            String shortLivedToken = request.get("short_lived_token");
            if (shortLivedToken == null || shortLivedToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing short_lived_token parameter"));
            }

            Map<String, String> params = Map.of(
                "grant_type", "fb_exchange_token",
                "client_id", appId,
                "client_secret", appSecret,
                "fb_exchange_token", shortLivedToken
            );

            JsonNode response = restClient.get()
                .uri("/oauth/access_token?" + buildQueryString(params))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            String token = response.get("access_token").asText();
            long expiresIn = response.get("expires_in").asLong();

            return ResponseEntity.ok(Map.of(
                "access_token", token,
                "expires_in", expiresIn
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/pages")
    public ResponseEntity<?> getPagesFromUser(@RequestParam String accessToken) {
        try {
            Map<String, String> params = Map.of(
                "fields", "id,name,access_token",
                "access_token", accessToken
            );

            JsonNode response = restClient.get()
                .uri("/me/accounts?" + buildQueryString(params))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            List<FacebookPage> pages = objectMapper.convertValue(response.get("data"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FacebookPage.class));

            return ResponseEntity.ok(pages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pages/{pageId}/events")
    public ResponseEntity<?> getPageEvents(@PathVariable String pageId, @RequestParam String accessToken) {
        try {
            Map<String, String> params = Map.of(
                "time_filter", "upcoming",
                "fields", "id,name,description,start_time,end_time,place,cover{source}",
                "access_token", accessToken
            );

            JsonNode response = restClient.get()
                .uri("/" + pageId + "/events?" + buildQueryString(params))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            List<FbEventResponse> events = objectMapper.convertValue(response.get("data"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FbEventResponse.class));

            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/oauth/refresh")
    public ResponseEntity<Map<String, Object>> refreshPageToken(@RequestBody Map<String, String> request) {
        try {
            String pageToken = request.get("page_token");
            if (pageToken == null || pageToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing page_token parameter"));
            }

            // Call the long-lived token method with the page token
            Map<String, String> longLivedRequest = Map.of("short_lived_token", pageToken);
            return getLongLivedToken(longLivedRequest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "&" + b)
            .orElse("");
    }

    // DTOs
    public record FacebookPage(String id, String name, String access_token) {}
    public record FbEventResponse(String id, String name, String description, String start_time, String end_time, FbPlace place, FbCover cover) {}
    public record FbPlace(String id, String name, FbLocation location) {}
    public record FbLocation(String street, String city, String zip, String country, Double latitude, Double longitude) {}
    public record FbCover(FbSource source) {}
    public record FbSource(String source) {}
}