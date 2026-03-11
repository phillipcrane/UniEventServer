package dk.unievent.web.mapper;

import dk.unievent.web.dto.PageDTO;
import dk.unievent.web.model.PageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PageMapper
 * Tests conversion between PageEntity ↔ PageDTO
 */
class PageMapperTests {
    
    private PageMapper pageMapper;
    
    private PageEntity testPageEntity;
    
    @BeforeEach
    void setUp() {
        pageMapper = new PageMapper();
        
        testPageEntity = new PageEntity();
        testPageEntity.setId("123456789");
        testPageEntity.setName("S-huset");
        testPageEntity.setPictureUrl("https://example.com/shuset.jpg");
        testPageEntity.setTokenStatus("valid");
        testPageEntity.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        testPageEntity.setLastRefreshAttempt(LocalDateTime.now());
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
        assertEquals("https://example.com/shuset.jpg", result.getPictureUrl());
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
        testPageEntity.setPictureUrl(null);
        
        PageDTO result = pageMapper.toDTO(testPageEntity);
        
        assertNotNull(result);
        assertNull(result.getPictureUrl());
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
        dto.setPictureUrl("https://example.com/new.jpg");
        dto.setUrl("https://facebook.com/page-new"); // Should be ignored in toEntity
        dto.setActive(true); // Should be ignored in toEntity
        
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("page-new", result.getId());
        assertEquals("New Organizer", result.getName());
        assertEquals("https://example.com/new.jpg", result.getPictureUrl());
        // These are not set by toEntity (computed by toDTO)
        assertNull(result.getTokenStatus());
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
        assertNull(result.getPictureUrl());
    }
    
    @Test
    void testToEntityWithNullPictureUrl() {
        PageDTO dto = new PageDTO();
        dto.setId("123");
        dto.setName("Test");
        dto.setPictureUrl(null);
        
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertNull(result.getPictureUrl());
    }
    
    @Test
    void testDTOtoEntityRoundTrip() {
        // Entity -> DTO -> Entity should preserve core fields
        PageDTO dto = pageMapper.toDTO(testPageEntity);
        PageEntity result = pageMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals(testPageEntity.getId(), result.getId());
        assertEquals(testPageEntity.getName(), result.getName());
        assertEquals(testPageEntity.getPictureUrl(), result.getPictureUrl());
    }
}
