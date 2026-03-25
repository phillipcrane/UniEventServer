package dk.unievent.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class FacebookService {

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

    public FacebookService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com/" + apiVersion).build();
        this.objectMapper = objectMapper;
    }

    public String getShortLivedToken(String code) {
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

        return response.get("access_token").asText();
    }

    public LongLivedToken getLongLivedToken(String shortLivedToken) {
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

        return new LongLivedToken(
            response.get("access_token").asText(),
            response.get("expires_in").asLong()
        );
    }

    public List<FacebookPage> getPagesFromUser(String userAccessToken) {
        Map<String, String> params = Map.of(
            "fields", "id,name,access_token",
            "access_token", userAccessToken
        );

        JsonNode response = restClient.get()
            .uri("/me/accounts?" + buildQueryString(params))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

        return objectMapper.convertValue(response.get("data"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, FacebookPage.class));
    }

    public List<FbEventResponse> getPageEvents(String pageId, String pageAccessToken) {
        Map<String, String> params = Map.of(
            "time_filter", "upcoming",
            "fields", "id,name,description,start_time,end_time,place,cover{source}",
            "access_token", pageAccessToken
        );

        JsonNode response = restClient.get()
            .uri("/" + pageId + "/events?" + buildQueryString(params))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

        return objectMapper.convertValue(response.get("data"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, FbEventResponse.class));
    }

    public LongLivedToken refreshPageToken(String pageToken) {
        return getLongLivedToken(pageToken);
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "&" + b)
            .orElse("");
    }

    public record LongLivedToken(String accessToken, long expiresIn) {}

    public record FacebookPage(String id, String name, String accessToken) {}

    public record FbEventResponse(String id, String name, String description, String start_time, String end_time, FbPlace place, FbCover cover) {}

    public record FbPlace(String id, String name, FbLocation location) {}

    public record FbLocation(String street, String city, String zip, String country, Double latitude, Double longitude) {}

    public record FbCover(FbSource source) {}

    public record FbSource(String source) {}
}