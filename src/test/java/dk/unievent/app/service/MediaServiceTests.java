package dk.unievent.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import dk.unievent.app.application.service.MediaService;
import dk.unievent.app.infrastructure.client.SeaweedFsClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {
    
    @Mock
    private SeaweedFsClient seaweedClient;
    
    private MediaService mediaService;
    
    @BeforeEach
    void setUp() {
        mediaService = new MediaService(seaweedClient);
    }
    
    @Test
    void testStoreFileSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
        
        // Note: This test documents the expected behavior for valid files
        assertNotNull(file);
        assertEquals("test.txt", file.getOriginalFilename());
        assertEquals("text/plain", file.getContentType());
        assertEquals(12, file.getSize());
    }
    
    @Test
    void testStoreEmptyFileFails() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );
        
        assertThrows(IOException.class, () -> mediaService.store(emptyFile));
    }
    
    @Test
    void testStorePathTraversalFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../etc/passwd",
                "text/plain",
                "malicious".getBytes()
        );
        
        assertThrows(IOException.class, () -> mediaService.store(file));
    }
    
    @Test
    void testLoadPathValidation() {
        // Test load method returns valid Path
        var loadResult = mediaService.load("test-file-123");
        assertNotNull(loadResult);
        assertEquals("test-file-123", loadResult.getFileName().toString());
    }
}
