package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.MediaDTO;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.infrastructure.client.SeaweedFsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {

    @Mock
    private SeaweedFsClient seaweedClient;

    @Mock
    private MediaRepository mediaRepository;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(seaweedClient, mediaRepository);
    }

    @Test
    void storeShouldUploadAllowedImageAndReturnFid() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        SeaweedFsClient.FileAssignment assignment = new SeaweedFsClient.FileAssignment("1,abc", "127.0.0.1:8080");
        when(seaweedClient.assignFile()).thenReturn(assignment);

        String fid = mediaService.store(file);

        assertEquals("1,abc", fid);
        verify(seaweedClient).uploadFile(eq("127.0.0.1:8080"), eq("1,abc"), eq("poster.png"), any(byte[].class));
    }

    @Test
    void storeShouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> mediaService.store(emptyFile));
    }

    @Test
    void storeShouldRejectPathTraversalFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "../../../etc/passwd", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});

        assertThrows(IllegalArgumentException.class, () -> mediaService.store(file));
    }

    @Test
    void storeShouldRejectUnsupportedContentBeforeAssigningStorage() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "not an image".getBytes());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> mediaService.store(file));

        assertTrue(exception.getMessage().contains("Unsupported file type"));
        verify(seaweedClient, never()).assignFile();
    }

    @Test
    void listAllShouldMapEntitiesToDtos() {
        MediaEntity entity = MediaEntity.builder()
                .id(7L)
                .filename("image.jpg")
                .contentType("image/jpeg")
                .fileId("1,def")
                .uploadedAt(Instant.now())
                .build();
        when(mediaRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));

        Page<MediaDTO> result = mediaService.listAll(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(7L, result.getContent().get(0).getId());
        assertEquals("image.jpg", result.getContent().get(0).getFilename());
    }

    @Test
    void findByIdShouldReturnEntityWhenFound() {
        MediaEntity entity = MediaEntity.builder().id(3L).filename("ok.png").build();
        when(mediaRepository.findById(3L)).thenReturn(Optional.of(entity));

        MediaEntity found = mediaService.findById(3L);

        assertEquals(3L, found.getId());
        assertEquals("ok.png", found.getFilename());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(mediaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> mediaService.findById(99L));
    }

    @Test
    void storeAndSaveShouldPersistSanitizedFilenameAndDetectedMimeType() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "my poster.png", "text/plain",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        SeaweedFsClient.FileAssignment assignment = new SeaweedFsClient.FileAssignment("1,xyz", "127.0.0.1:8080");
        when(seaweedClient.assignFile()).thenReturn(assignment);
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(invocation -> {
            MediaEntity saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        MediaDTO result = mediaService.storeAndSave(file);

        assertEquals(11L, result.getId());
        assertEquals("my_poster.png", result.getFilename());
        assertEquals("image/png", result.getContentType());
        assertEquals("1,xyz", result.getFileId());
        verify(mediaRepository).save(any(MediaEntity.class));
    }

    @Test
    void storeAndSaveShouldNotPersistMetadataWhenStorageUploadFails() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        SeaweedFsClient.FileAssignment assignment = new SeaweedFsClient.FileAssignment("1,fail", "127.0.0.1:8080");
        when(seaweedClient.assignFile()).thenReturn(assignment);
        doThrow(new IOException("volume unavailable"))
                .when(seaweedClient)
                .uploadFile(eq("127.0.0.1:8080"), eq("1,fail"), eq("poster.png"), any(byte[].class));

        IOException exception = assertThrows(IOException.class, () -> mediaService.storeAndSave(file));

        assertTrue(exception.getMessage().contains("Error uploading file to SeaweedFS"));
        verify(mediaRepository, never()).save(any(MediaEntity.class));
    }

    @Test
    void storeShouldHandleConcurrentImageUploadsWithDistinctAssignments() throws Exception {
        AtomicInteger sequence = new AtomicInteger();
        Set<String> uploadedFileIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        when(seaweedClient.assignFile()).thenAnswer(invocation ->
                new SeaweedFsClient.FileAssignment("1," + sequence.incrementAndGet(), "127.0.0.1:8080"));
        org.mockito.Mockito.doAnswer(invocation -> {
            uploadedFileIds.add(invocation.getArgument(1));
            return null;
        }).when(seaweedClient).uploadFile(eq("127.0.0.1:8080"), any(), any(), any(byte[].class));

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int index = i;
                futures.add(executor.submit(() -> mediaService.store(new MockMultipartFile(
                        "file",
                        "poster-" + index + ".png",
                        "image/png",
                        new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'}))));
            }

            Set<String> returnedFileIds = new HashSet<>();
            for (Future<String> future : futures) {
                returnedFileIds.add(future.get(2, TimeUnit.SECONDS));
            }

            assertEquals(8, returnedFileIds.size());
            assertEquals(returnedFileIds, uploadedFileIds);
            verify(seaweedClient, times(8)).assignFile();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void loadShouldReturnPathForCompatibility() {
        var loadResult = mediaService.load("test-file-123");

        assertNotNull(loadResult);
        assertEquals("test-file-123", loadResult.getFileName().toString());
    }

    @Test
    void loadAsResourceShouldReturnDownloadedBytes() throws IOException {
        byte[] imageBytes = new byte[] {1, 2, 3};
        when(seaweedClient.downloadFile("1,file")).thenReturn(imageBytes);

        var resource = mediaService.loadAsResource("1,file");

        assertArrayEquals(imageBytes, resource.getInputStream().readAllBytes());
    }

    @Test
    void loadAsResourceShouldRejectBlankFileId() throws IOException {
        IOException exception = assertThrows(IOException.class, () -> mediaService.loadAsResource(""));

        assertEquals("Invalid file ID", exception.getMessage());
        verify(seaweedClient, never()).downloadFile(any());
    }

    @Test
    void loadAsResourceShouldWrapUnexpectedStorageFailures() throws IOException {
        when(seaweedClient.downloadFile("1,boom")).thenThrow(new IllegalStateException("lookup failed"));

        IOException exception = assertThrows(IOException.class, () -> mediaService.loadAsResource("1,boom"));

        assertEquals("Could not read file: 1,boom", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void deleteShouldRejectBlankFileIdBeforeStorageCall() throws IOException {
        IOException exception = assertThrows(IOException.class, () -> mediaService.delete(""));

        assertEquals("Invalid file ID", exception.getMessage());
        verify(seaweedClient, never()).deleteFile(any());
    }

    @Test
    void deleteShouldIgnoreStorageFailures() throws IOException {
        doThrow(new IOException("already deleted")).when(seaweedClient).deleteFile("1,missing");

        assertDoesNotThrow(() -> mediaService.delete("1,missing"));

        verify(seaweedClient).deleteFile("1,missing");
    }

    @Test
    void downloadAndStoreImageShouldRejectNonHttpsUrlsBeforeStorage() throws IOException {
        mediaService.buildAllowlist();

        IOException exception = assertThrows(IOException.class,
                () -> mediaService.downloadAndStoreImage("http://facebook.com/image.jpg", "image.jpg"));

        assertTrue(exception.getMessage().contains("Only HTTPS image URLs are permitted"));
        verify(seaweedClient, never()).assignFile();
    }

    @Test
    void downloadAndStoreImageShouldRejectHostsOutsideAllowlistBeforeStorage() throws IOException {
        mediaService.buildAllowlist();

        IOException exception = assertThrows(IOException.class,
                () -> mediaService.downloadAndStoreImage("https://example.com/image.jpg", "image.jpg"));

        assertEquals("Image URL host not in allowlist: example.com", exception.getMessage());
        verify(seaweedClient, never()).assignFile();
    }
}
