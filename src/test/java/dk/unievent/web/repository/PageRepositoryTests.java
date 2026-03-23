package dk.unievent.web.repository;

import dk.unievent.web.model.PageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PageRepositoryTests {

    @Autowired
    private PageRepository pageRepository;
    
    private PageEntity testPage;
    
    @BeforeEach
    void setUp() {
        testPage = new PageEntity();
        testPage.setId("page-1");
        testPage.setName("Test Page");
        testPage.setTokenStatus("valid");
        testPage.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        pageRepository.save(testPage);

    }
    
    @Test
    void testSavePage() {
        PageEntity page = new PageEntity();
        page.setId("page-2");
        page.setName("New Page");
        page.setTokenStatus("valid");
        
        PageEntity savedPage = pageRepository.save(page);
        
        assertNotNull(savedPage);
        assertEquals("page-2", savedPage.getId());
        assertEquals("New Page", savedPage.getName());
        assertNotNull(savedPage.getCreatedAt());
        assertNotNull(savedPage.getConnectedAt());
    }
    
    @Test
    void testFindPageById() {
        Optional<PageEntity> foundPage = pageRepository.findById("page-1");
        
        assertTrue(foundPage.isPresent());
        assertEquals("Test Page", foundPage.get().getName());
        assertEquals("page-1", foundPage.get().getId());
    }
    
    @Test
    void testFindPageByIdNotFound() {
        Optional<PageEntity> foundPage = pageRepository.findById("non-existent");
        
        assertTrue(foundPage.isEmpty());
    }
    
    @Test
    void testFindAllPages() {
        PageEntity page2 = new PageEntity();
        page2.setId("page-2");
        page2.setName("Second Page");
        page2.setTokenStatus("valid");
        pageRepository.save(page2);

        
        List<PageEntity> pages = pageRepository.findAll();
        
        assertEquals(2, pages.size());
    }
    
    @Test
    void testFindAllByOrderByNameAsc() {
        PageEntity page2 = new PageEntity();
        page2.setId("page-2");
        page2.setName("Alpha Page");
        page2.setTokenStatus("valid");
        pageRepository.save(page2);
        
        PageEntity page3 = new PageEntity();
        page3.setId("page-3");
        page3.setName("Zebra Page");
        page3.setTokenStatus("valid");
        pageRepository.save(page3);

        
        List<PageEntity> pages = pageRepository.findAllByOrderByNameAsc();
        
        assertEquals(3, pages.size());
        assertEquals("Alpha Page", pages.get(0).getName());
        assertEquals("Test Page", pages.get(1).getName());
        assertEquals("Zebra Page", pages.get(2).getName());
    }
    
    @Test
    void testFindByTokenStatusOrderByNameAsc() {
        PageEntity invalidPage = new PageEntity();
        invalidPage.setId("page-invalid");
        invalidPage.setName("Invalid Page");
        invalidPage.setTokenStatus("invalid");
        pageRepository.save(invalidPage);
        
        PageEntity validPage = new PageEntity();
        validPage.setId("page-valid");
        validPage.setName("Another Valid");
        validPage.setTokenStatus("valid");
        pageRepository.save(validPage);

        
        List<PageEntity> validPages = pageRepository.findByTokenStatusOrderByNameAsc("valid");
        
        assertEquals(2, validPages.size());
        assertTrue(validPages.stream().allMatch(p -> "valid".equals(p.getTokenStatus())));
        assertEquals("Another Valid", validPages.get(0).getName());
        assertEquals("Test Page", validPages.get(1).getName());
    }
    
    @Test
    void testFindByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc() {
        LocalDateTime now = LocalDateTime.now();
        
        PageEntity expiredPage = new PageEntity();
        expiredPage.setId("page-expired");
        expiredPage.setName("Expired Page");
        expiredPage.setTokenExpiresAt(now.minusDays(1));
        pageRepository.save(expiredPage);
        
        PageEntity futureExpirePage = new PageEntity();
        futureExpirePage.setId("page-future");
        futureExpirePage.setName("Future Expire");
        futureExpirePage.setTokenExpiresAt(now.plusDays(10));
        pageRepository.save(futureExpirePage);

        
        List<PageEntity> expiredPages = pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(now);
        
        assertEquals(1, expiredPages.size());
        assertEquals("page-expired", expiredPages.get(0).getId());
    }
    
    @Test
    void testUpdatePage() {
        PageEntity page = pageRepository.findById("page-1").orElseThrow();
        LocalDateTime originalUpdatedAt = page.getUpdatedAt();
        
        page.setName("Updated Page Name");
        page.setTokenStatus("expired");
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        pageRepository.save(page);

        
        PageEntity updatedPage = pageRepository.findById("page-1").orElseThrow();
        
        assertEquals("Updated Page Name", updatedPage.getName());
        assertEquals("expired", updatedPage.getTokenStatus());
        assertTrue(updatedPage.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updatedPage.getUpdatedAt().isEqual(originalUpdatedAt));
    }
    
    @Test
    void testDeletePage() {
        pageRepository.deleteById("page-1");

        
        Optional<PageEntity> deletedPage = pageRepository.findById("page-1");
        
        assertTrue(deletedPage.isEmpty());
    }
    
    @Test
    void testPageTokenManagement() {
        PageEntity page = new PageEntity();
        page.setId("page-token");
        page.setName("Token Test");
        
        LocalDateTime refreshedAt = LocalDateTime.now();
        page.setTokenRefreshedAt(refreshedAt);
        page.setTokenStatus("valid");
        page.setTokenExpiresInDays(30);
        
        pageRepository.save(page);

        
        PageEntity retrieved = pageRepository.findById("page-token").orElseThrow();
        
        assertEquals(refreshedAt, retrieved.getTokenRefreshedAt());
        assertEquals("valid", retrieved.getTokenStatus());
        assertEquals(30, retrieved.getTokenExpiresInDays());
    }
    
    @Test
    void testPageRefreshTracking() {
        PageEntity page = new PageEntity();
        page.setId("page-refresh");
        page.setName("Refresh Test");
        
        LocalDateTime lastAttempt = LocalDateTime.now();
        page.setLastRefreshAttempt(lastAttempt);
        page.setLastRefreshSuccess(true);
        
        pageRepository.save(page);

        
        PageEntity retrieved = pageRepository.findById("page-refresh").orElseThrow();
        
        assertEquals(lastAttempt, retrieved.getLastRefreshAttempt());
        assertTrue(retrieved.getLastRefreshSuccess());
    }
    
    @Test
    void testPageWithNullFields() {
        PageEntity page = new PageEntity();
        page.setId("page-null");
        page.setName("Null Fields Test");
        // Other fields remain null
        
        pageRepository.save(page);

        
        PageEntity retrieved = pageRepository.findById("page-null").orElseThrow();
        
        assertNull(retrieved.getTokenStatus());
        assertNull(retrieved.getLastRefreshSuccess());
        // Picture field is MediaEntity, not URL
        assertNull(retrieved.getPicture());
    }
}

