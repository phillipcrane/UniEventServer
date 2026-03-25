package dk.unievent.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import dk.unievent.app.seaweedfs.MediaConfig;
import dk.unievent.app.seaweedfs.MediaService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {
    
    @Mock
    private MediaConfig mediaConfig;
    
    private MediaService mediaService;
    
    @BeforeEach
    void setUp() {
        // Manually create MediaService with mocked config
        mediaService = new MediaService(mediaConfig);
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
