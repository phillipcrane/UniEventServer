package dk.unievent.web.service;

import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretManagerServiceTest {

    @Mock
    private SecretManagerServiceClient client;

    private SecretManagerService secretManagerService;

    @BeforeEach
    void setUp() throws Exception {
        // Mock the static create() method
        try (MockedStatic<SecretManagerServiceClient> mockedStatic = mockStatic(SecretManagerServiceClient.class)) {
            mockedStatic.when(SecretManagerServiceClient::create).thenReturn(client);
            secretManagerService = new SecretManagerService();
        }

        // Set test project ID
        ReflectionTestUtils.setField(secretManagerService, "projectId", "test-project");
    }

    @Test
    void addPageToken_shouldCreateSecretAndAddVersion() throws Exception {
        // Arrange
        String pageId = "test-page";
        String token = "test-token";
        long expiresIn = 3600L;

        // Act
        secretManagerService.addPageToken(pageId, token, expiresIn);

        // Assert
        verify(client).createSecret(eq("projects/test-project"), eq("facebook-token-test-page"), any(Secret.class));
        verify(client).addSecretVersion(anyString(), any(SecretPayload.class));
    }

    @Test
    void addPageToken_shouldHandleAlreadyExistsException() throws Exception {
        // Arrange
        String pageId = "test-page";
        String token = "test-token";
        long expiresIn = 3600L;

        doThrow(new RuntimeException("Already exists")).when(client)
            .createSecret(anyString(), anyString(), any(Secret.class));

        // Act & Assert
        assertDoesNotThrow(() -> secretManagerService.addPageToken(pageId, token, expiresIn));
        verify(client).createSecret(anyString(), anyString(), any(Secret.class));
        verify(client).addSecretVersion(anyString(), any(SecretPayload.class));
    }

    @Test
    void addPageToken_shouldThrowExceptionForOtherErrors() throws Exception {
        // Arrange
        String pageId = "test-page";
        String token = "test-token";
        long expiresIn = 3600L;

        doThrow(new RuntimeException("Some other error")).when(client)
            .createSecret(anyString(), anyString(), any(Secret.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> secretManagerService.addPageToken(pageId, token, expiresIn));
    }

    @Test
    void updatePageToken_shouldCallAddPageToken() throws Exception {
        // Arrange
        String pageId = "test-page";
        String token = "test-token";
        long expiresIn = 3600L;

        SecretManagerService spyService = spy(secretManagerService);
        doNothing().when(spyService).addPageToken(pageId, token, expiresIn);

        // Act
        spyService.updatePageToken(pageId, token, expiresIn);

        // Assert
        verify(spyService).addPageToken(pageId, token, expiresIn);
    }

    @Test
    void getPageToken_shouldReturnToken() throws Exception {
        // Arrange
        String pageId = "test-page";
        String expectedToken = "retrieved-token";

        AccessSecretVersionResponse response = mock(AccessSecretVersionResponse.class);
        SecretPayload payload = mock(SecretPayload.class);
        ByteString data = ByteString.copyFromUtf8(expectedToken);

        when(client.accessSecretVersion(anyString())).thenReturn(response);
        when(response.getPayload()).thenReturn(payload);
        when(payload.getData()).thenReturn(data);

        // Act
        String result = secretManagerService.getPageToken(pageId);

        // Assert
        assertEquals(expectedToken, result);
        verify(client).accessSecretVersion(anyString());
    }

    @Test
    void getPageToken_shouldReturnNullWhenNotFound() throws Exception {
        // Arrange
        String pageId = "test-page";

        when(client.accessSecretVersion(anyString()))
            .thenThrow(new RuntimeException("NOT_FOUND"));

        // Act
        String result = secretManagerService.getPageToken(pageId);

        // Assert
        assertNull(result);
    }

    @Test
    void getPageToken_shouldThrowExceptionForOtherErrors() throws Exception {
        // Arrange
        String pageId = "test-page";

        when(client.accessSecretVersion(anyString()))
            .thenThrow(new RuntimeException("Some other error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> secretManagerService.getPageToken(pageId));
    }

    @Test
    void checkTokenExpiry_shouldReturnFalse() {
        // Act
        boolean result = secretManagerService.checkTokenExpiry("test-page");

        // Assert
        assertFalse(result);
    }
}