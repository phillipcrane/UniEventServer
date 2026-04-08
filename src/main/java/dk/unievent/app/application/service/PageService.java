package dk.unievent.app.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.mapper.PageMapper;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class PageService {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final MediaService mediaService;
    private final MediaRepository mediaRepository;

    public PageService(
            PageRepository pageRepository,
            PageMapper pageMapper,
            MediaService mediaService,
            MediaRepository mediaRepository) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
    }

    public Page<PageDTO> getAllPages(Pageable pageable) {
        log.debug("Fetching all pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> result = pageRepository.findAllByOrderByNameAsc(pageable)
            .map(pageMapper::toDTO);
        log.debug("Found {} pages", result.getTotalElements());
        return result;
    }

    public PageDTO getPageById(String id) {
        log.debug("Fetching page with id: {}", id); 
        Optional<PageEntity> entity = pageRepository.findById(id);
        if (entity.isEmpty()) {
            log.debug("Page not found with id: {}", id);
        } else {
            log.debug("Page found: {}", id);
        }
        return entity.map(pageMapper::toDTO).orElse(null);
    }

    public Page<PageDTO> getActivePages(Pageable pageable) {
        log.debug("Fetching active pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> result = pageRepository.findByTokenStatusOrderByNameAsc("valid", pageable)
            .map(pageMapper::toDTO);
        log.debug("Found {} active pages", result.getTotalElements());
        return result;
    }

    public Page<PageEntity> getPagesToRefresh(Pageable pageable) {
        return pageRepository.findPagesToRefresh(LocalDateTime.now(), pageable);
    }

    public Page<PageDTO> getExpiredPages(Pageable pageable) {
        return pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(LocalDateTime.now(), pageable)
            .map(pageMapper::toDTO);
    }

    public Page<PageDTO> searchPagesByName(String name, Pageable pageable) {
        log.debug("Searching pages by name: {}", name);
        Page<PageDTO> result = pageRepository.findByNameIgnoreCase(name, pageable)
            .map(pageMapper::toDTO);
        log.debug("Search found {} pages matching: {}", result.getTotalElements(), name);
        return result;
    }

    public PageDTO savePage(PageDTO pageDTO) {
        log.info("Saving page: {}", pageDTO.getName());
        PageEntity entity = pageMapper.toEntity(pageDTO);

        if (pageDTO.getPictureId() != null) {
            MediaEntity picture = mediaRepository.findById(pageDTO.getPictureId()).orElse(null);
            entity.setPicture(picture);
        }

        PageEntity saved = pageRepository.save(entity);
        log.info("Page saved successfully with id: {}", saved.getId());
        return pageMapper.toDTO(saved);
    }

    public void updatePageToken(String pageId, String tokenStatus, LocalDateTime expiresAt, Integer expiresInDays) {
        log.debug("Updating token for page: {}, status: {}", pageId, tokenStatus);
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
            log.info("Page token updated successfully: {}", pageId);
        } else {
            log.warn("Page not found for token update: {}", pageId);
        }
    }

    public void logRefreshFailure(String pageId, String error) {
        log.warn("Logging refresh failure for page: {}, error: {}", pageId, error);
        Optional<PageEntity> existing = pageRepository.findById(pageId);
        if (existing.isPresent()) {
            PageEntity page = existing.get();
            page.setLastRefreshSuccess(false);
            page.setLastRefreshError(error);
            page.setLastRefreshAttempt(LocalDateTime.now());
            pageRepository.save(page);
        }        
    }

    public PageDTO uploadPicture(String id, MultipartFile file) throws IOException {
        log.info("Uploading picture for page: {}", id);
        Optional<PageEntity> existing = pageRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Page not found for picture upload with id: {}", id);
            return null;
        }

        String storedFilename = mediaService.store(file);
        MediaEntity mediaEntity = MediaEntity.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileId(storedFilename)
                .uploadedAt(Instant.now())
                .build();
        MediaEntity saved = mediaRepository.save(mediaEntity);

        PageEntity page = existing.get();
        page.setPicture(saved);
        PageEntity updated = pageRepository.save(page);
        log.info("Picture uploaded successfully for page: {}", id);

        return pageMapper.toDTO(updated);
    }

    public boolean deletePage(String id) {
        log.info("Deleting page with id: {}", id);
        if (pageRepository.existsById(id)) {
            pageRepository.deleteById(id);
            log.info("Page deleted successfully: {}", id);
            return true;
        }
        log.warn("Page not found for deletion with id: {}", id);
        return false;
    }
}
