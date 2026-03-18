package dk.unievent.web.controller;

import dk.unievent.web.entity.Event;
import dk.unievent.web.entity.Page;
import dk.unievent.web.repository.EventRepository;
import dk.unievent.web.repository.PageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;


@RestController
@RequestMapping("/api")
public class EventController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PageRepository pageRepository;
    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.facebook.url:http://localhost:8081}")
    private String facebookServiceUrl;

    @Value("${services.core.url:http://localhost:8082}")
    private String coreServiceUrl;

    @Value("${services.storage.url:http://localhost:8083}")
    private String storageServiceUrl;

    private String secretManagerServiceUrl;

    public EventController(PageRepository pageRepository,
                          EventRepository eventRepository,
                          ObjectMapper objectMapper) {
        this(pageRepository, eventRepository, objectMapper, "http://localhost:8081", "http://localhost:8082", "http://localhost:8083");
    }

    public EventController(PageRepository pageRepository,
                          EventRepository eventRepository,
                          ObjectMapper objectMapper,
                          String facebookServiceUrl,
                          String coreServiceUrl,
                          String storageServiceUrl) {
        this.pageRepository = pageRepository;
        this.eventRepository = eventRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.facebookServiceUrl = facebookServiceUrl;
        this.coreServiceUrl = coreServiceUrl;
        this.storageServiceUrl = storageServiceUrl;
        this.secretManagerServiceUrl = coreServiceUrl;
    }

    @PostMapping("/callback")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody Map<String, String> input) {
        String code = input.get("code");
        boolean debug = Boolean.parseBoolean(input.get("debug"));

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing code"));
        }

        try {
            // Get short-lived token from Facebook service
            Map<String, Object> shortLivedRequest = Map.of("code", code);
            ResponseEntity<Map<String, Object>> shortLivedResponse = restTemplate.exchange(
                facebookServiceUrl + "/api/facebook/oauth/token",
                HttpMethod.POST,
                new HttpEntity<>(shortLivedRequest),
                MAP_TYPE
            );

            if (!shortLivedResponse.getStatusCode().is2xxSuccessful() || shortLivedResponse.getBody() == null) {
                return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to get short-lived token"));
            }

            String shortLivedToken = (String) shortLivedResponse.getBody().get("access_token");

            // Exchange for long-lived token
            Map<String, Object> longLivedRequest = Map.of("short_lived_token", shortLivedToken);
            ResponseEntity<Map<String, Object>> longLivedResponse = restTemplate.exchange(
                facebookServiceUrl + "/api/facebook/oauth/long-lived-token",
                HttpMethod.POST,
                new HttpEntity<>(longLivedRequest),
                MAP_TYPE
            );

            if (!longLivedResponse.getStatusCode().is2xxSuccessful() || longLivedResponse.getBody() == null) {
                return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to get long-lived token"));
            }

            String longLivedToken = (String) longLivedResponse.getBody().get("access_token");
            Long expiresIn = ((Number) longLivedResponse.getBody().get("expires_in")).longValue();
            List<Map<String, Object>> pages = (List<Map<String, Object>>) restTemplate.getForObject(
                facebookServiceUrl + "/api/facebook/user/pages?accessToken=" + longLivedToken,
                List.class
            );

            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "storedPages", 0, "message", "No pages returned."));
            }

            int storedPages = 0;
            for (Map<String, Object> pageData : pages) {
                try {
                    // Store token in Secret Manager service
                    Map<String, Object> tokenRequest = Map.of(
                        "token", pageData.get("access_token"),
                        "expiresIn", expiresIn
                    );
                    restTemplate.postForEntity(
                        coreServiceUrl + "/api/secrets/pages/" + pageData.get("id") + "/token",
                        tokenRequest,
                        Map.class
                    );

                    Page page = new Page();
                    page.setId((String) pageData.get("id"));
                    page.setName((String) pageData.get("name"));
                    page.setActive(true);
                    page.setUrl("https://facebook.com/" + pageData.get("id"));
                    page.setConnectedAt(Instant.now());
                    page.setTokenRefreshedAt(Instant.now());
                    page.setTokenStoredAt(Instant.now());
                    page.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
                    page.setTokenExpiresInDays((long) Math.ceil(expiresIn / (60.0 * 60 * 24)));
                    page.setTokenStatus("valid");
                    page.setLastRefreshSuccess(true);

                    pageRepository.save(page);
                    storedPages++;
                } catch (Exception e) {
                    // Log error
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "storedPages", storedPages, "message", "Stored " + storedPages + " page token(s)."));
        } catch (Exception e) {
            String msg = e.getMessage();
            return ResponseEntity.status(500).body(Map.of("success", false, "error", msg, "message", debug ? msg : "Facebook auth failed"));
        }
    }

    @PostMapping("/ingest")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleIngest() {
        try {
            List<Page> pages = pageRepository.findAll();
            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("totalPages", 0, "totalEvents", 0, "duration", 0));
            }

            long startTime = System.currentTimeMillis();
            int totalEventsProcessed = 0;
            List<Map<String, Object>> pageResults = new ArrayList<>();

            for (Page page : pages) {
                long pageStartTime = System.currentTimeMillis();
                try {
                    // Get token from Secret Manager service
                    ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        HttpMethod.GET,
                        null,
                        MAP_TYPE
                    );

                    String token = null;
                    if (tokenResponse.getStatusCode().is2xxSuccessful() && tokenResponse.getBody() != null) {
                        token = (String) tokenResponse.getBody().get("token");
                    }

                    if (token == null) {
                        pageResults.add(Map.of(
                            "pageId", page.getId(),
                            "pageName", page.getName(),
                            "status", "skipped",
                            "reason", "no_token",
                            "duration", System.currentTimeMillis() - pageStartTime
                        ));
                        continue;
                    }

                    // Get events from Facebook service
                    List<Map<String, Object>> events = (List<Map<String, Object>>) restTemplate.getForObject(
                        facebookServiceUrl + "/api/facebook/pages/" + page.getId() + "/events?accessToken=" + token,
                        List.class
                    );
                    if (events.isEmpty()) {
                        pageResults.add(Map.of(
                            "pageId", page.getId(),
                            "pageName", page.getName(),
                            "status", "success",
                            "eventsProcessed", 0,
                            "eventsFailed", 0,
                            "duration", System.currentTimeMillis() - pageStartTime
                        ));
                        continue;
                    }

                    List<Event> eventsData = new ArrayList<>();
                    for (Map<String, Object> eventData : events) {
                        try {
                            String coverImageUrl = null;
                            Map<String, Object> cover = (Map<String, Object>) eventData.get("cover");
                            if (cover != null) {
                                Map<String, Object> source = (Map<String, Object>) cover.get("source");
                                if (source != null && source.get("source") != null) {
                                    // Call Storage service to add image from URL
                                    Map<String, Object> imageRequest = Map.of(
                                        "filePath", "covers/" + page.getId() + "/" + eventData.get("id") + ".jpg",
                                        "sourceUrl", source.get("source")
                                    );
                                    ResponseEntity<Map<String, Object>> imageResponse = restTemplate.exchange(
                                        storageServiceUrl + "/api/storage/images/from-url",
                                        HttpMethod.POST,
                                        new HttpEntity<>(imageRequest),
                                        MAP_TYPE
                                    );
                                    if (imageResponse.getStatusCode().is2xxSuccessful()) {
                                        // For now, we'll skip setting the coverImageUrl since the storage service doesn't fully implement URL downloading
                                    }
                                }
                            }

                            String placeJson = null;
                            Map<String, Object> place = (Map<String, Object>) eventData.get("place");
                            if (place != null) {
                                try {
                                    placeJson = objectMapper.writeValueAsString(place);
                                } catch (Exception e) {
                                    // If serialization fails, store as string representation
                                    placeJson = place.toString();
                                }
                            }

                            Instant eventStartTime = null;
                            Instant eventEndTime = null;
                            try {
                                String startTimeStr = (String) eventData.get("start_time");
                                if (startTimeStr != null) {
                                    eventStartTime = Instant.parse(startTimeStr);
                                }
                            } catch (Exception e) {
                                // If parsing fails, keep as null
                            }
                            try {
                                String endTimeStr = (String) eventData.get("end_time");
                                if (endTimeStr != null) {
                                    eventEndTime = Instant.parse(endTimeStr);
                                }
                            } catch (Exception e) {
                                // If parsing fails, keep as null
                            }

                            String rawJson = null;
                            try {
                                rawJson = objectMapper.writeValueAsString(eventData);
                            } catch (Exception e) {
                                // If serialization fails, skip
                            }

                            Event event = new Event();
                            event.setId((String) eventData.get("id"));
                            event.setPageId(page.getId());
                            event.setTitle((String) eventData.get("name"));
                            event.setDescription((String) eventData.get("description"));
                            event.setStartTime(eventStartTime);
                            event.setEndTime(eventEndTime);
                            event.setPlace(placeJson);
                            event.setCoverImageUrl(coverImageUrl);
                            event.setEventURL("https://facebook.com/events/" + eventData.get("id"));
                            event.setCreatedAt(Instant.now());
                            event.setUpdatedAt(Instant.now());
                            event.setRaw(rawJson);
                            eventsData.add(event);
                        } catch (Exception e) {
                            // Handle error
                        }
                    }

                    eventRepository.saveAll(eventsData);
                    totalEventsProcessed += eventsData.size();
                    pageResults.add(Map.of(
                        "pageId", page.getId(),
                        "pageName", page.getName(),
                        "status", "success",
                        "eventsProcessed", eventsData.size(),
                        "eventsFailed", events.size() - eventsData.size(),
                        "duration", System.currentTimeMillis() - pageStartTime
                    ));
                } catch (Exception e) {
                    pageResults.add(Map.of(
                        "pageId", page.getId(),
                        "pageName", page.getName(),
                        "status", "failed",
                        "error", e.getMessage(),
                        "duration", System.currentTimeMillis() - pageStartTime
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "totalPages", pages.size(),
                "totalEvents", totalEventsProcessed,
                "duration", System.currentTimeMillis() - startTime,
                "pageResults", pageResults
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-tokens")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleRefreshTokens() {
        try {
            List<Page> pages = pageRepository.findAll();
            if (pages.isEmpty()) {
                return ResponseEntity.ok(Map.of("tokensRefreshed", 0, "tokensFailed", 0, "durationMs", 0));
            }

            long startTime = System.currentTimeMillis();
            int tokensRefreshed = 0;
            int tokensFailed = 0;

            for (Page page : pages) {
                try {
                    // Get current token from Secret Manager service
                    ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        HttpMethod.GET,
                        null,
                        MAP_TYPE
                    );

                    String currentToken = null;
                    if (tokenResponse.getStatusCode().is2xxSuccessful() && tokenResponse.getBody() != null) {
                        currentToken = (String) tokenResponse.getBody().get("token");
                    }

                    if (currentToken == null) {
                        throw new RuntimeException("No token found");
                    }

                    // Refresh token using Facebook service
                    Map<String, Object> refreshRequest = Map.of("page_token", currentToken);
                    ResponseEntity<Map<String, Object>> refreshResponse = restTemplate.exchange(
                        facebookServiceUrl + "/api/facebook/oauth/refresh",
                        HttpMethod.POST,
                        new HttpEntity<>(refreshRequest),
                        MAP_TYPE
                    );

                    if (!refreshResponse.getStatusCode().is2xxSuccessful() || refreshResponse.getBody() == null) {
                        throw new RuntimeException("Failed to refresh token");
                    }

                    String newToken = (String) refreshResponse.getBody().get("access_token");
                    Long expiresIn = ((Number) refreshResponse.getBody().get("expires_in")).longValue();

                    // Update token in Secret Manager service
                    Map<String, Object> updateRequest = Map.of(
                        "token", newToken,
                        "expiresIn", expiresIn
                    );
                    restTemplate.put(
                        secretManagerServiceUrl + "/api/secrets/pages/" + page.getId() + "/token",
                        updateRequest
                    );

                    page.setTokenRefreshedAt(Instant.now());
                    page.setLastRefreshSuccess(true);
                    page.setLastRefreshError(null);
                    pageRepository.save(page);
                    tokensRefreshed++;
                } catch (Exception e) {
                    tokensFailed++;
                    try {
                        page.setLastRefreshSuccess(false);
                        page.setLastRefreshError(e.getMessage());
                        page.setLastRefreshAttempt(Instant.now());
                        pageRepository.save(page);
                    } catch (Exception dbErr) {
                        // Silent fail
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "tokensRefreshed", tokensRefreshed,
                "tokensFailed", tokensFailed,
                "durationMs", System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}