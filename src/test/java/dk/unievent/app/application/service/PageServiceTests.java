package dk.unievent.app.application.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.mapper.PageMapper;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;

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

    @Mock
    private MediaService mediaService;

    @Mock
    private MediaRepository mediaRepository;

    private PageService pageService;

    private PageEntity testPageEntity;
    private PageDTO testPageDTO;

    @BeforeEach
    void setUp() {
        pageService = new PageService(pageRepository, pageMapper, mediaService, mediaRepository, Optional.empty());
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
        Page<PageEntity> pageResultPage = new PageImpl<>(pageEntities, PageRequest.of(0, 20), 2);
        when(pageRepository.findAllByOrderByNameAsc(any(Pageable.class))).thenReturn(pageResultPage);
        
        PageDTO dto2 = new PageDTO();
        dto2.setId("page-2");
        dto2.setName("Page 2");
        
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        when(pageMapper.toDTO(page2)).thenReturn(dto2);
        
        Page<PageDTO> result = pageService.getAllPages(PageRequest.of(0, 20));
        
        assertEquals(2, result.getContent().size());
        assertEquals("page-1", result.getContent().get(0).getId());
        verify(pageRepository, times(1)).findAllByOrderByNameAsc(any(Pageable.class));
        verify(pageMapper, times(2)).toDTO(any());
    }
    
    @Test
    void testGetAllPagesEmpty() {
        Page<PageEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(pageRepository.findAllByOrderByNameAsc(any(Pageable.class))).thenReturn(emptyPage);
        
        Page<PageDTO> result = pageService.getAllPages(PageRequest.of(0, 20));
        
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void testGetPageById() {
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPageEntity));
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        Optional<PageDTO> result = pageService.getPageById("page-1");

        assertTrue(result.isPresent());
        assertEquals("page-1", result.get().getId());
        assertEquals("Test Page", result.get().getName());
        verify(pageRepository, times(1)).findById("page-1");
        verify(pageMapper, times(1)).toDTO(testPageEntity);
    }
    
    @Test
    void testGetPageByIdNotFound() {
        when(pageRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        Optional<PageDTO> result = pageService.getPageById("non-existent");

        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetActivePages() {
        List<PageEntity> activePages = List.of(testPageEntity);
        Page<PageEntity> activePagesPage = new PageImpl<>(activePages, PageRequest.of(0, 20), 1);
        when(pageRepository.findByTokenStatusOrderByNameAsc(eq("valid"), any(Pageable.class)))
            .thenReturn(activePagesPage);
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        Page<PageDTO> result = pageService.getActivePages(PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        assertEquals("page-1", result.getContent().get(0).getId());
        verify(pageRepository, times(1)).findByTokenStatusOrderByNameAsc(eq("valid"), any(Pageable.class));
    }
    
    @Test
    void testGetActivePagesEmpty() {
        Page<PageEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(pageRepository.findByTokenStatusOrderByNameAsc(eq("valid"), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        Page<PageDTO> result = pageService.getActivePages(PageRequest.of(0, 20));
        
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void testGetExpiredPages() {
        PageEntity expiredPage = new PageEntity();
        expiredPage.setId("page-expired");
        expiredPage.setName("Expired");
        expiredPage.setTokenExpiresAt(LocalDateTime.now().minusDays(1));
        
        List<PageEntity> expiredPages = List.of(expiredPage);
        Page<PageEntity> expiredPagesPage = new PageImpl<>(expiredPages, PageRequest.of(0, 20), 1);
        when(pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(expiredPagesPage);
        
        PageDTO expiredDTO = new PageDTO();
        expiredDTO.setId("page-expired");
        expiredDTO.setName("Expired");
        when(pageMapper.toDTO(expiredPage)).thenReturn(expiredDTO);
        
        Page<PageDTO> result = pageService.getExpiredPages(PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        verify(pageRepository, times(1))
            .findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(any(LocalDateTime.class), any(Pageable.class));
    }
    
    @Test
    void testSearchPagesByName() {
        List<PageEntity> searchResults = List.of(testPageEntity);
        Page<PageEntity> searchResultsPage = new PageImpl<>(searchResults, PageRequest.of(0, 20), 1);
        when(pageRepository.findByNameIgnoreCase(eq("test page"), any(Pageable.class)))
            .thenReturn(searchResultsPage);
        when(pageMapper.toDTO(testPageEntity)).thenReturn(testPageDTO);
        
        Page<PageDTO> result = pageService.searchPagesByName("test page", PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        verify(pageRepository, times(1)).findByNameIgnoreCase(eq("test page"), any(Pageable.class));
    }
    
    @Test
    void testSearchPagesByNameEmpty() {
        Page<PageEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(pageRepository.findByNameIgnoreCase(eq("not-found"), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        Page<PageDTO> result = pageService.searchPagesByName("not-found", PageRequest.of(0, 20));
        
        assertTrue(result.getContent().isEmpty());
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
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(testPageEntity));

        boolean result = pageService.deletePage("page-1");

        assertTrue(result);
        verify(pageRepository, times(1)).findById("page-1");
        verify(pageRepository, times(1)).deleteById("page-1");
    }

    @Test
    void testDeletePageNotFound() {
        when(pageRepository.findById("non-existent")).thenReturn(Optional.empty());

        boolean result = pageService.deletePage("non-existent");

        assertFalse(result);
        verify(pageRepository, times(1)).findById("non-existent");
        verify(pageRepository, never()).deleteById(anyString());
    }
    
    @Test
    void testGetPagesToRefresh() {
        List<PageEntity> pagesToRefresh = List.of(testPageEntity);
        Page<PageEntity> pagesToRefreshPage = new PageImpl<>(pagesToRefresh, PageRequest.of(0, 20), 1);
        when(pageRepository.findPagesToRefresh(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(pagesToRefreshPage);
        
        Page<PageEntity> result = pageService.getPagesToRefresh(PageRequest.of(0, 20));
        
        assertEquals(1, result.getContent().size());
        verify(pageRepository, times(1)).findPagesToRefresh(any(LocalDateTime.class), any(Pageable.class));
    }
}
