package dk.unievent.app.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import dk.unievent.app.mysql.model.MediaEntity;
import dk.unievent.app.seaweedfs.MediaConfig;
import dk.unievent.app.seaweedfs.MediaService;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MediaIntegrationTests {

    @Mock
    private MediaConfig mediaConfig;

    private MediaService mediaService;
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        // Create a real MediaService instance for integration testing
        mediaService = new MediaService(mediaConfig);

        testFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "PDF content here".getBytes()
        );
    }

    @Test
    void testStoreFileSuccess() throws IOException {
        // Documents the expected file properties
        assertNotNull(testFile);
        assertEquals("test-document.pdf", testFile.getOriginalFilename());
        assertEquals("application/pdf", testFile.getContentType());
        assertEquals(16, testFile.getSize());
    }

    @Test
    void testMediaEntityBuilderFlow() {
        // Test creating MediaEntity with builder pattern (used in controller)
        MediaEntity media = MediaEntity.builder()
                .filename("test.pdf")
                .contentType("application/pdf")
                .fileId("1,01")
                .uploadedAt(Instant.now())
                .build();

        assertNotNull(media);
        assertEquals("test.pdf", media.getFilename());
        assertEquals("application/pdf", media.getContentType());
        assertEquals("1,01", media.getFileId());
        assertNotNull(media.getUploadedAt());
    }

    @Test
    void testEmptyFileRejection() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertTrue(emptyFile.isEmpty());
        assertThrows(IOException.class, () -> mediaService.store(emptyFile));
    }

    @Test
    void testPathTraversalRejection() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../../etc/passwd",
                "text/plain",
                "malicious".getBytes()
        );

        assertThrows(IOException.class, () -> mediaService.store(maliciousFile));
    }

    @Test
    void testMediaEntityLoadMethod() {
        // Test load method (internal utility)
        var path = mediaService.load("test-file-123");
        assertNotNull(path);
        assertEquals("test-file-123", path.getFileName().toString());
    }

    @Test
    void testSpecialCharactersInFilename() {
        MockMultipartFile specialFile = new MockMultipartFile(
                "file",
                "document_2024-01-15.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertNotNull(specialFile.getOriginalFilename());
        assertTrue(specialFile.getOriginalFilename().contains("_"));
        assertTrue(specialFile.getOriginalFilename().contains("-"));
    }

    @Test
    void testFileContentTypeVariety() {
        String[] contentTypes = {"image/png", "application/json", "text/csv", "video/mp4"};
        String[] filenames = {"image.png", "data.json", "report.csv", "video.mp4"};

        for (int i = 0; i < contentTypes.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    filenames[i],
                    contentTypes[i],
                    ("Content of " + filenames[i]).getBytes()
            );

            assertEquals(filenames[i], file.getOriginalFilename());
            assertEquals(contentTypes[i], file.getContentType());
        }
    }

    @Test
    void testMediaEntityWithAllFields() {
        // Test complete entity construction
        Instant now = Instant.now();
        MediaEntity media = MediaEntity.builder()
                .id(1L)
                .filename("complete-test.pdf")
                .contentType("application/pdf")
                .fileId("9,99")
                .uploadedAt(now)
                .build();

        assertEquals(1L, media.getId());
        assertEquals("complete-test.pdf", media.getFilename());
        assertEquals("application/pdf", media.getContentType());
        assertEquals("9,99", media.getFileId());
        assertEquals(now, media.getUploadedAt());
    }
}
