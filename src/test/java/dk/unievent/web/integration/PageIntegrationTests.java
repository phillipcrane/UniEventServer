package dk.unievent.web.integration;

import dk.unievent.web.dto.PageDTO;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.repository.PageRepository;
import dk.unievent.web.service.PageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Page (Organizer) operations
 * Tests full stack: Database ↔ Repository ↔ Service ↔ Mapper ↔ DTO
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PageIntegrationTests {
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private PageService pageService;
    
    // ============= Create Tests =============
    
    @Test
    void testCreatePageAndRetrieveAsDTO() {
        PageDTO createDTO = new PageDTO();
        createDTO.setId("page-new");
        createDTO.setName("New Organizer");
        createDTO.setPictureUrl("https://example.com/picture.jpg");
        
        PageDTO created = pageService.savePage(createDTO);
        
        assertNotNull(created);
        assertEquals("page-new", created.getId());
        assertEquals("New Organizer", created.getName());
        assertEquals("https://example.com/picture.jpg", created.getPictureUrl());
    }
    
    @Test
    void testPagePersistenceInDatabase() {
        PageDTO createDTO = new PageDTO();
        createDTO.setId("persist-test");
        createDTO.setName("Persist Test Page");
        
        pageService.savePage(createDTO);
        
        // Verify in database
        Optional<PageEntity> dbEntity = pageRepository.findById("persist-test");
        assertTrue(dbEntity.isPresent());
        assertEquals("Persist Test Page", dbEntity.get().getName());
    }
    
    // ============= Retrieve Tests =============
    
    @Test
    void testGetAllPagesThroughService() {
        createTestPage("page-1", "Page One");
        createTestPage("page-2", "Page Two");
        createTestPage("page-3", "Page Three");
        
        List<PageDTO> allPages = pageService.getAllPages();
        
        assertEquals(3, allPages.size());
        assertTrue(allPages.stream().anyMatch(p -> "Page One".equals(p.getName())));
        assertTrue(allPages.stream().anyMatch(p -> "Page Two".equals(p.getName())));
        assertTrue(allPages.stream().anyMatch(p -> "Page Three".equals(p.getName())));
    }
    
    @Test
    void testGetActivePages() {
        PageEntity page1 = createDatabasePage("page-1", "Active Page", "valid");
        PageEntity page2 = createDatabasePage("page-2", "Inactive Page", "expired");
        PageEntity page3 = createDatabasePage("page-3", "Another Active", "valid");
        
        List<PageDTO> activePages = pageService.getActivePages();
        
        assertEquals(2, activePages.size());
        assertTrue(activePages.stream().allMatch(PageDTO::getActive));
    }
    
    @Test
    void testGetPageById() {
        PageDTO created = createTestPage("page-search", "Search Me");
        
        PageDTO found = pageService.getPageById("page-search");
        
        assertNotNull(found);
        assertEquals("Search Me", found.getName());
    }
    
    @Test
    void testGetPageByIdNotFoundReturnsNull() {
        PageDTO notFound = pageService.getPageById("nonexistent");
        
        assertNull(notFound);
    }
    
    @Test
    void testSearchPagesByName() {
        createTestPage("page-1", "S-huset");
        createTestPage("page-2", "Pumpehuset");
        createTestPage("page-3", "Slotsholmen");
        
        List<PageDTO> searchResults = pageService.searchPagesByName("s-huset");
        
        assertEquals(1, searchResults.size());
        assertEquals("S-huset", searchResults.get(0).getName());
    }
    
    @Test
    void testSearchPagesByNamePartial() {
        createTestPage("page-1", "S-huset");
        createTestPage("page-2", "S-huset");
        createTestPage("page-3", "Pumpehuset");
        
        List<PageDTO> searchResults = pageService.searchPagesByName("S-huset");
        
        assertEquals(2, searchResults.size());
    }
    
    // ============= Update Tests =============
    
    @Test
    void testUpdatePageThroughService() {
        PageDTO created = createTestPage("page-update", "Original Name");
        
        PageDTO updateDTO = new PageDTO();
        updateDTO.setId("page-update");
        updateDTO.setName("Updated Name");
        updateDTO.setPictureUrl("https://example.com/new.jpg");
        
        PageDTO updated = pageService.savePage(updateDTO);
        
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
    }
    
    @Test
    void testUpdatePagePersistsToDatabase() {
        createTestPage("page-persist", "Before Update");
        
        PageDTO updateDTO = new PageDTO();
        updateDTO.setId("page-persist");
        updateDTO.setName("After Update");
        
        pageService.savePage(updateDTO);
        
        // Verify in database
        Optional<PageEntity> dbEntity = pageRepository.findById("page-persist");
        assertTrue(dbEntity.isPresent());
        assertEquals("After Update", dbEntity.get().getName());
    }
    
    // ============= Delete Tests =============
    
    @Test
    void testDeletePageThroughService() {
        createTestPage("page-delete", "Delete Me");
        
        boolean deleted = pageService.deletePage("page-delete");
        
        assertTrue(deleted);
        // Verify deleted from database
        assertFalse(pageRepository.findById("page-delete").isPresent());
    }
    
    @Test
    void testDeletePageNotFoundReturnsFalse() {
        boolean deleted = pageService.deletePage("nonexistent");
        
        assertFalse(deleted);
    }
    
    // ============= Helper Methods =============
    
    private PageDTO createTestPage(String id, String name) {
        PageDTO dto = new PageDTO();
        dto.setId(id);
        dto.setName(name);
        return pageService.savePage(dto);
    }
    
    private PageEntity createDatabasePage(String id, String name, String tokenStatus) {
        PageEntity entity = new PageEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setTokenStatus(tokenStatus);
        entity.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        return pageRepository.save(entity);
    }
}
