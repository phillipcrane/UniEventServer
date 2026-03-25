package dk.unievent.app.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.unievent.app.mysql.model.MediaEntity;
import dk.unievent.app.mysql.repository.MediaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRepositoryTests {
    
    @Mock
    private MediaRepository mediaRepository;
    
    private MediaEntity testMedia;
    
    @BeforeEach
    void setUp() {
        testMedia = MediaEntity.builder()
                .id(1L)
                .filename("test.txt")
                .contentType("text/plain")
                .fileId("1,01")
                .uploadedAt(Instant.now())
                .build();
    }
    
    @Test
    void testSaveMedia() {
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(testMedia);
        
        MediaEntity saved = mediaRepository.save(testMedia);
        
        assertNotNull(saved);
        assertEquals("test.txt", saved.getFilename());
        assertEquals("1,01", saved.getFileId());
        verify(mediaRepository, times(1)).save(testMedia);
    }
    
    @Test
    void testFindById() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(testMedia));
        
        Optional<MediaEntity> found = mediaRepository.findById(1L);
        
        assertTrue(found.isPresent());
        assertEquals("test.txt", found.get().getFilename());
        verify(mediaRepository, times(1)).findById(1L);
    }
    
    @Test
    void testFindByIdNotFound() {
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());
        
        Optional<MediaEntity> found = mediaRepository.findById(999L);
        
        assertFalse(found.isPresent());
        verify(mediaRepository, times(1)).findById(999L);
    }
    
    @Test
    void testFindAll() {
        MediaEntity media2 = MediaEntity.builder()
                .id(2L)
                .filename("document.pdf")
                .contentType("application/pdf")
                .fileId("2,01")
                .build();
        
        List<MediaEntity> mediaList = List.of(testMedia, media2);
        when(mediaRepository.findAll()).thenReturn(mediaList);
        
        List<MediaEntity> result = mediaRepository.findAll();
        
        assertEquals(2, result.size());
        assertEquals("test.txt", result.get(0).getFilename());
        assertEquals("document.pdf", result.get(1).getFilename());
        verify(mediaRepository, times(1)).findAll();
    }
    
    @Test
    void testFindAllEmpty() {
        when(mediaRepository.findAll()).thenReturn(List.of());
        
        List<MediaEntity> result = mediaRepository.findAll();
        
        assertTrue(result.isEmpty());
        verify(mediaRepository, times(1)).findAll();
    }
    
    @Test
    void testDeleteById() {
        doNothing().when(mediaRepository).deleteById(1L);
        
        mediaRepository.deleteById(1L);
        
        verify(mediaRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void testUpdateMedia() {
        testMedia.setFilename("updated.txt");
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(testMedia);
        
        MediaEntity updated = mediaRepository.save(testMedia);
        
        assertEquals("updated.txt", updated.getFilename());
        verify(mediaRepository, times(1)).save(testMedia);
    }
}
