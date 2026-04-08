package dk.unievent.app.mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.mapper.PageMapper;
import dk.unievent.app.db.model.PageEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PageMapper
 * Tests conversion between PageEntity ↔ PageDTO
 */
@ExtendWith(MockitoExtension.class)
class PageMapperTests {
    
    @InjectMocks
    private PageMapper pageMapper;
    
    private PageEntity testPageEntity;
    
    @BeforeEach
    void setUp() {
        testPageEntity = PageEntity.builder()
                .id("123456789")
                .name("S-huset")
                .tokenStatus("valid")
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .lastRefreshAttempt(LocalDateTime.now())
                .build();
    }
    
    // ============= toDTO Tests =============
    
    @Test
    void testToDTOWithNull() {
        PageDTO result = pageMapper.toDTO(null);
        
        assertNull(result);
    }
    
    @Test
    void testToDTOWithActiveEntity() {
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertNotNull(result);
        assertEquals("123456789", result.getId());
        assertEquals("S-huset", result.getName());
        // Picture ID is mapped from MediaEntity relationship
        assertNull(result.getPictureId());
        assertEquals("https://facebook.com/123456789", result.getUrl());
        assertTrue(result.getActive());
    }
    
    @Test
    void testToDTOWithInactiveEntity() {
        testPageEntity.setTokenStatus("expired");
        
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertNotNull(result);
        assertEquals("123456789", result.getId());
        assertEquals("S-huset", result.getName());
        assertFalse(result.getActive());
    }
    
    @Test
    void testToDTOWithNullTokenStatus() {
        testPageEntity.setTokenStatus(null);
        
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertNotNull(result);
        assertFalse(result.getActive());
    }
    
    @Test
    void testToDTOComputesUrlWithId() {
        testPageEntity.setId("987654321");
        
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertEquals("https://facebook.com/987654321", result.getUrl());
    }
    
    @Test
    void testToDTOWithNullPictureUrl() {
        // Picture is a MediaEntity object, not a URL
        
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertNotNull(result);
        assertNull(result.getPictureId());
        assertEquals("123456789", result.getId());
    }
    
    @Test
    void testToDTOMultipleValidTokenStatuses() {
        // Only "valid" should result in active = true
        testPageEntity.setTokenStatus("valid");
        assertTrue(pageMapper.toDTO(testPageEntity).getActive());
        
        testPageEntity.setTokenStatus("invalid");
        assertFalse(pageMapper.toDTO(testPageEntity).getActive());
        
        testPageEntity.setTokenStatus("VALID");
        assertFalse(pageMapper.toDTO(testPageEntity).getActive()); // Case sensitive
    }
    
    // ============= toEntity Tests =============
    
    @Test
    void testToEntityWithNull() {
        PageEntity result = pageMapper.toEntity(null);
        
        assertNull(result);
    }
    
    @Test
    void testToEntityWithFullDTO() {
        PageDTO dto = new PageDTO();
        dto.setId("page-new");
        dto.setName("New Organizer");
        dto.setPictureId(1L);
        dto.setUrl("https://facebook.com/page-new"); // Should be ignored in toEntity
        dto.setActive(true); // Should be ignored in toEntity
        
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("page-new", result.getId());
        assertEquals("New Organizer", result.getName());
        assertNull(result.getPicture());
    }
    
    @Test
    void testToEntityWithMinimalDTO() {
        PageDTO dto = new PageDTO();
        dto.setId("page-minimal");
        dto.setName("Minimal Page");
        
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("page-minimal", result.getId());
        assertEquals("Minimal Page", result.getName());
        assertNull(result.getPicture());
    }
    
    @Test
    void testToEntityWithNullPictureUrl() {
        PageDTO dto = new PageDTO();
        dto.setId("123");
        dto.setName("Test");
        dto.setPictureId(null);
        
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        // Picture field is MediaEntity, not URL
        assertNull(result.getPicture());
    }
    
    @Test
    void testDTOtoEntityRoundTrip() {
        // Entity -> DTO -> Entity should preserve core fields
        PageDTO dto = pageMapper.toDTO(testPageEntity);
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals(testPageEntity.getId(), result.getId());
        assertEquals(testPageEntity.getName(), result.getName());
        // Picture mapping comparison skipped - entity uses MediaEntity object
    }
}
