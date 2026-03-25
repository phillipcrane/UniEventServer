package dk.unievent.web.service;

import dk.unievent.web.dto.PageDTO;
import dk.unievent.web.mapper.PageMapper;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.repository.PageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageServiceTests {
    
    @Mock
    private PageRepository pageRepository;
    
    @Mock
    private PageMapper pageMapper;
    
    @InjectMocks
    private PageService pageService;
    
    private PageEntity testPageEntity;
    private PageDTO testPageDTO;
    
    @BeforeEach
    void setUp() {
        testPageEntity = PageEntity.builder()
                .id("page-1")
                .name("Test Page")
                .tokenStatus("valid")
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .build();
        
        testPageDTO = new PageDTO();
        testPageDTO.setId("page-1");
        testPageDTO.setName("Test Page");
        testPageDTO.setActive(true);
    }
    
    @Test
    void testGetAllPages() {
        PageEntity page2 = PageEntity.builder()
                .id("page-2")
                .name("Page 2")
                .build();
        
        List<PageEntity> pageEntities = List.of(testPageEntity, page2);
        when(pageRepository.findAllByOrderByNameAsc()).thenReturn(pageEntities);
        
        PageDTO dto2 = new PageDTO();
        dto2.setId("page-2");
        dto2.setName("Page 2");
        
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        when(pageMapper.toDTO(page2)).thenReturn(dto2);
        
        List<PageDTO> result = pageService.getAllPages();
        
        assertEquals(2, result.size());
        assertEquals("page-1", result.get(0).getId());
        verify(pageRepository, times(1)).findAllByOrderByNameAsc();
        verify(pageMapper, times(2)).toDTO(any());
    }
    
    @Test
    void testGetAllPagesEmpty() {
        when(pageRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        
        List<PageDTO> result = pageService.getAllPages();
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetPageById() {
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPageEntity));
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        PageDTO result = pageService.getPageById("page-1");
        
        assertNotNull(result);
        assertEquals("page-1", result.getId());
        assertEquals("Test Page", result.getName());
        verify(pageRepository, times(1)).findById("page-1");
        verify(pageMapper, times(1)).toDTO(testPageEntity);
    }
    
    @Test
    void testGetPageByIdNotFound() {
        when(pageRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        PageDTO result = pageService.getPageById("non-existent");
        
        assertNull(result);
    }
    
    @Test
    void testGetActivePages() {
        List<PageEntity> activePages = List.of(testPageEntity);
        when(pageRepository.findByTokenStatusOrderByNameAsc("valid"))
            .thenReturn(activePages);
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        List<PageDTO> result = pageService.getActivePages();
        
        assertEquals(1, result.size());
        assertEquals("page-1", result.get(0).getId());
        verify(pageRepository, times(1)).findByTokenStatusOrderByNameAsc("valid");
    }
    
    @Test
    void testGetActivePagesEmpty() {
        when(pageRepository.findByTokenStatusOrderByNameAsc("valid"))
            .thenReturn(List.of());
        
        List<PageDTO> result = pageService.getActivePages();
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetExpiredPages() {
        PageEntity expiredPage = new PageEntity();
        expiredPage.setId("page-expired");
        expiredPage.setName("Expired");
        expiredPage.setTokenExpiresAt(LocalDateTime.now().minusDays(1));
        
        List<PageEntity> expiredPages = List.of(expiredPage);
        when(pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(any(LocalDateTime.class)))
            .thenReturn(expiredPages);
        
        PageDTO expiredDTO = new PageDTO();
        expiredDTO.setId("page-expired");
        expiredDTO.setName("Expired");
        when(pageMapper.toDTO(expiredPage)).thenReturn(expiredDTO);
        
        List<PageDTO> result = pageService.getExpiredPages();
        
        assertEquals(1, result.size());
        verify(pageRepository, times(1))
            .findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(any(LocalDateTime.class));
    }
    
    @Test
    void testSearchPagesByName() {
        List<PageEntity> searchResults = List.of(testPageEntity);
        when(pageRepository.findByNameIgnoreCase("test page"))
            .thenReturn(searchResults);
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        List<PageDTO> result = pageService.searchPagesByName("test page");
        
        assertEquals(1, result.size());
        verify(pageRepository, times(1)).findByNameIgnoreCase("test page");
    }
    
    @Test
    void testSearchPagesByNameEmpty() {
        when(pageRepository.findByNameIgnoreCase("not-found"))
            .thenReturn(List.of());
        
        List<PageDTO> result = pageService.searchPagesByName("not-found");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testSavePage() {
        when(pageMapper.toEntity(testPageDTO)).thenReturn(testPageEntity);
        when(pageRepository.save(testPageEntity)).thenReturn(testPageEntity);
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        PageDTO result = pageService.savePage(testPageDTO);
        
        assertNotNull(result);
        assertEquals("page-1", result.getId());
        verify(pageMapper, times(1)).toEntity(testPageDTO);
        verify(pageRepository, times(1)).save(testPageEntity);
        verify(pageMapper, times(1)).toDTO(testPageEntity);
    }
    
    @Test
    void testUpdatePageToken() {
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPageEntity));
        
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(60);
        pageService.updatePageToken("page-1", "valid", newExpiry, 60);
        
        verify(pageRepository, times(1)).findById("page-1");
        verify(pageRepository, times(1)).save(any(PageEntity.class));
    }
    
    @Test
    void testUpdatePageTokenNotFound() {
        when(pageRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        pageService.updatePageToken("non-existent", "valid", LocalDateTime.now(), 30);
        
        verify(pageRepository, times(1)).findById("non-existent");
        verify(pageRepository, never()).save(any());
    }
    
    @Test
    void testLogRefreshFailure() {
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPageEntity));
        
        pageService.logRefreshFailure("page-1", "Token refresh failed");
        
        verify(pageRepository, times(1)).findById("page-1");
        verify(pageRepository, times(1)).save(any(PageEntity.class));
    }
    
    @Test
    void testDeletePage() {
        when(pageRepository.existsById("page-1")).thenReturn(true);
        
        boolean result = pageService.deletePage("page-1");
        
        assertTrue(result);
        verify(pageRepository, times(1)).deleteById("page-1");
    }
    
    @Test
    void testDeletePageNotFound() {
        when(pageRepository.existsById("non-existent")).thenReturn(false);
        
        boolean result = pageService.deletePage("non-existent");
        
        assertFalse(result);
        verify(pageRepository, never()).deleteById(anyString());
    }
    
    @Test
    void testGetPagesToRefresh() {
        List<PageEntity> pagesToRefresh = List.of(testPageEntity);
        when(pageRepository.findPagesToRefresh(any(LocalDateTime.class)))
            .thenReturn(pagesToRefresh);
        
        List<PageEntity> result = pageService.getPagesToRefresh();
        
        assertEquals(1, result.size());
        verify(pageRepository, times(1)).findPagesToRefresh(any(LocalDateTime.class));
    }
}
