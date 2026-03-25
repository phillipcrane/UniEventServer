package dk.unievent.app.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.unievent.app.core.dto.PageDTO;
import dk.unievent.app.core.mapper.PageMapper;
import dk.unievent.app.mysql.model.PageEntity;
import dk.unievent.app.mysql.repository.PageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PageService {
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private PageMapper pageMapper;
    
    /**
     * Get all pages ordered by name
     */
    public List<PageDTO> getAllPages() {
        return pageRepository.findAllByOrderByNameAsc()
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a page by ID
     */
    public PageDTO getPageById(String id) {
        Optional<PageEntity> entity = pageRepository.findById(id);
        return entity.map(pageMapper::toDTO).orElse(null);
    }
    
    /**
     * Get all active pages (tokenStatus = "valid")
     */
    public List<PageDTO> getActivePages() {
        return pageRepository.findByTokenStatusOrderByNameAsc("valid")
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pages that need token refresh (tokens expiring soon)
     * Used by the scheduled refresh job
     */
    public List<PageEntity> getPagesToRefresh() {
        return pageRepository.findPagesToRefresh(LocalDateTime.now());
    }
    
    /**
     * Get pages with expired tokens
     */
    public List<PageDTO> getExpiredPages() {
        return pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(LocalDateTime.now())
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Search pages by name
     */
    public List<PageDTO> searchPagesByName(String name) {
        return pageRepository.findByNameIgnoreCase(name)
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Create or update a page
     */
    public PageDTO savePage(PageDTO pageDTO) {
        PageEntity entity = pageMapper.toEntity(pageDTO);
        PageEntity saved = pageRepository.save(entity);
        return pageMapper.toDTO(saved);
    }
    
    /**
     * Update page token information
     */
    public void updatePageToken(String pageId, String tokenStatus, LocalDateTime expiresAt, Integer expiresInDays) {
        Optional<PageEntity> existing = pageRepository.findById(pageId);
        if (existing.isPresent()) {
            PageEntity page = existing.get();
            page.setTokenStatus(tokenStatus);
            page.setTokenExpiresAt(expiresAt);
            page.setTokenExpiresInDays(expiresInDays);
            page.setTokenRefreshedAt(LocalDateTime.now());
            page.setLastRefreshSuccess(true);
            page.setLastRefreshError(null);
            page.setLastRefreshAttempt(LocalDateTime.now());
            pageRepository.save(page);
        }
    }
    
    /**
     * Log a failed refresh attempt
     */
    public void logRefreshFailure(String pageId, String error) {
        Optional<PageEntity> existing = pageRepository.findById(pageId);
        if (existing.isPresent()) {
            PageEntity page = existing.get();
            page.setLastRefreshSuccess(false);
            page.setLastRefreshError(error);
            page.setLastRefreshAttempt(LocalDateTime.now());
            pageRepository.save(page);
        }
    }
    
    /**
     * Delete a page and all its associated events
     */
    public boolean deletePage(String id) {
        if (pageRepository.existsById(id)) {
            pageRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
