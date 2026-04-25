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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void loadShouldReturnPathForCompatibility() {
        var loadResult = mediaService.load("test-file-123");

        assertNotNull(loadResult);
        assertEquals("test-file-123", loadResult.getFileName().toString());
    }
}
