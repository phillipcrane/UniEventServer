package dk.unievent.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacebookServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private com.fasterxml.jackson.databind.type.TypeFactory typeFactory;

    @Mock
    private com.fasterxml.jackson.databind.type.CollectionType collectionType;

    private FacebookService facebookService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        facebookService = new FacebookService(restClientBuilder, objectMapper);

        // Set test values using reflection
        ReflectionTestUtils.setField(facebookService, "apiVersion", "v23.0");
        ReflectionTestUtils.setField(facebookService, "appId", "test-app-id");
        ReflectionTestUtils.setField(facebookService, "appSecret", "test-app-secret");
        ReflectionTestUtils.setField(facebookService, "redirectUri", "http://localhost/callback");

        lenient().when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
        lenient().when(typeFactory.constructCollectionType(any(Class.class), any(Class.class))).thenReturn(collectionType);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getShortLivedToken_shouldReturnToken() {
        // Arrange
        String code = "test-code";
        String expectedToken = "short-lived-token";

        when(jsonNode.get("access_token")).thenReturn(jsonNode);
        when(jsonNode.asText()).thenReturn(expectedToken);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(jsonNode);

        // Act
        String result = facebookService.getShortLivedToken(code);

        // Assert
        assertEquals(expectedToken, result);
        verify(restClient).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLongLivedToken_shouldReturnLongLivedToken() {
        // Arrange
        String shortLivedToken = "short-token";
        String expectedToken = "long-lived-token";
        long expectedExpiresIn = 3600L;

        when(jsonNode.get("access_token")).thenReturn(jsonNode);
        when(jsonNode.asText()).thenReturn(expectedToken);
        when(jsonNode.get("expires_in")).thenReturn(jsonNode);
        when(jsonNode.asLong()).thenReturn(expectedExpiresIn);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(jsonNode);

        // Act
        FacebookService.LongLivedToken result = facebookService.getLongLivedToken(shortLivedToken);

        // Assert
        assertEquals(expectedToken, result.accessToken());
        assertEquals(expectedExpiresIn, result.expiresIn());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagesFromUser_shouldReturnListOfPages() {
        // Arrange
        String userAccessToken = "user-token";
        List<FacebookService.FacebookPage> expectedPages = List.of(
            new FacebookService.FacebookPage("page1", "Page One", "page-token-1"),
            new FacebookService.FacebookPage("page2", "Page Two", "page-token-2")
        );

        when(jsonNode.get("data")).thenReturn(jsonNode);
        when(objectMapper.convertValue(eq(jsonNode), any(com.fasterxml.jackson.databind.JavaType.class))).thenReturn(expectedPages);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(jsonNode);

        // Act
        List<FacebookService.FacebookPage> result = facebookService.getPagesFromUser(userAccessToken);

        // Assert
        assertEquals(expectedPages, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPageEvents_shouldReturnListOfEvents() {
        // Arrange
        String pageId = "test-page-id";
        String pageAccessToken = "page-token";
        List<FacebookService.FbEventResponse> expectedEvents = List.of(
            new FacebookService.FbEventResponse("event1", "Event One", "Description", "2024-01-01", "2024-01-02", null, null)
        );

        when(jsonNode.get("data")).thenReturn(jsonNode);
        when(objectMapper.convertValue(eq(jsonNode), any(com.fasterxml.jackson.databind.JavaType.class))).thenReturn(expectedEvents);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(jsonNode);

        // Act
        List<FacebookService.FbEventResponse> result = facebookService.getPageEvents(pageId, pageAccessToken);

        // Assert
        assertEquals(expectedEvents, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshPageToken_shouldCallGetLongLivedToken() {
        // Arrange
        String pageToken = "page-token";
        FacebookService.LongLivedToken expectedToken = new FacebookService.LongLivedToken("refreshed-token", 3600L);

        when(jsonNode.get("access_token")).thenReturn(jsonNode);
        when(jsonNode.asText()).thenReturn("refreshed-token");
        when(jsonNode.get("expires_in")).thenReturn(jsonNode);
        when(jsonNode.asLong()).thenReturn(3600L);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any(MediaType.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(jsonNode);

        // Act
        FacebookService.LongLivedToken result = facebookService.refreshPageToken(pageToken);

        // Assert
        assertEquals(expectedToken.accessToken(), result.accessToken());
        assertEquals(expectedToken.expiresIn(), result.expiresIn());
    }
}