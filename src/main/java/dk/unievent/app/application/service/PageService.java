package dk.unievent.app.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.api.dto.FbPageResponse;
import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.mapper.PageMapper;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.infrastructure.constants.TokenConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

// CRUD for Facebook pages and their token metadata. token status tracking lives here (valid, expired,
// error) so the scheduler knows which pages to skip and the admin dashboard can show token health.
@Slf4j
@Service
public class PageService {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final MediaService mediaService;
    private final MediaRepository mediaRepository;
    private final Optional<VaultService> vaultService;

    public PageService(
            PageRepository pageRepository,
            PageMapper pageMapper,
            MediaService mediaService,
            MediaRepository mediaRepository,
            Optional<VaultService> vaultService) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
        this.vaultService = vaultService;
    }

    public Page<PageDTO> getAllPages(Pageable pageable) {
        log.debug("Fetching all pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> result = pageRepository.findAllByOrderByNameAsc(pageable)
            .map(pageMapper::toDTO);
        log.debug("Found {} pages", result.getTotalElements());
        return result;
    }

    public Optional<PageDTO> getPageById(String id) {
        log.debug("Fetching page with id: {}", id); 
        Optional<PageEntity> entity = pageRepository.findById(id);
        if (entity.isEmpty()) {
            log.debug("Page not found with id: {}", id);
        } else {
            log.debug("Page found: {}", id);
        }
        return entity.map(pageMapper::toDTO);
    }

    public Page<PageDTO> getActivePages(Pageable pageable) {
        log.debug("Fetching active pages with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PageDTO> result = pageRepository.findByTokenStatusOrderByNameAsc("valid", pageable)
            .map(pageMapper::toDTO);
        log.debug("Found {} active pages", result.getTotalElements());
        return result;
    }

    public Page<PageEntity> getPagesToRefresh(Pageable pageable) {
        return pageRepository.findPagesToRefresh(LocalDateTime.now().plusDays(TokenConstants.REFRESH_WINDOW_DAYS), pageable);
    }

    public Page<PageEntity> getAllPageEntities(Pageable pageable) {
        return pageRepository.findAllByOrderByNameAsc(pageable);
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

    public Optional<PageDTO> updatePage(String id, PageDTO pageDTO) {
        return pageRepository.findById(id).map(existing -> {
            existing.setName(pageDTO.getName());
            if (pageDTO.getPictureId() != null) {
                MediaEntity picture = mediaRepository.findById(pageDTO.getPictureId()).orElse(null);
                existing.setPicture(picture);
            }
            return pageMapper.toDTO(pageRepository.save(existing));
        });
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

    @Transactional
    public Optional<PageDTO> uploadPicture(String id, MultipartFile file) throws IOException {
        log.info("Uploading picture for page: {}", id);
        Optional<PageEntity> existing = pageRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Page not found for picture upload with id: {}", id);
            return Optional.empty();
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
        MediaEntity oldMedia = page.getPicture();
        page.setPicture(saved);
        PageEntity updated = pageRepository.save(page);

        if (oldMedia != null) {
            try {
                mediaService.delete(oldMedia.getFileId());
            } catch (IOException e) {
                log.warn("Failed to delete old picture from SeaweedFS: {}", oldMedia.getFileId(), e);
            }
        }

        log.info("Picture uploaded successfully for page: {}", id);
        return Optional.of(pageMapper.toDTO(updated));
    }

    @Transactional
    public boolean deletePage(String id) {
        log.info("Deleting page with id: {}", id);
        Optional<PageEntity> page = pageRepository.findById(id);
        if (page.isEmpty()) {
            log.warn("Page not found for deletion with id: {}", id);
            return false;
        }
        MediaEntity picture = page.get().getPicture();
        pageRepository.deleteById(id);
        vaultService.ifPresent(v -> v.markPageTokenInactive(id));
        if (picture != null) {
            try {
                mediaService.delete(picture.getFileId());
            } catch (java.io.IOException e) {
                log.warn("Failed to delete picture from SeaweedFS: {}", picture.getFileId(), e);
            }
        }
        log.info("Page deleted successfully: {}", id);
        return true;
    }

    // upserts a PageEntity from the OAuth flow. sets token status to valid and initialises
    // the expiration to 60 days out (Facebook long-lived token lifetime).
    public PageEntity createOrUpdatePageFromFacebook(FbPageResponse fbPageResponse) {
        log.debug("Processing Facebook page: {} ({})", fbPageResponse.getName(), fbPageResponse.getId());

        Optional<PageEntity> existing = pageRepository.findById(fbPageResponse.getId());

        PageEntity pageEntity;
        if (existing.isPresent()) {
            pageEntity = existing.get();
            log.debug("Updating existing page: {}", fbPageResponse.getId());
        } else {
            pageEntity = new PageEntity();
            pageEntity.setId(fbPageResponse.getId());
            log.debug("Creating new page from Facebook: {}", fbPageResponse.getId());
        }

        pageEntity.setName(fbPageResponse.getName());
        pageEntity.setTokenStatus("valid");
        pageEntity.setTokenRefreshedAt(LocalDateTime.now());
        pageEntity.setLastRefreshSuccess(true);
        pageEntity.setLastRefreshError(null);
        pageEntity.setLastRefreshAttempt(LocalDateTime.now());
        pageEntity.setTokenExpiresAt(LocalDateTime.now().plusDays(TokenConstants.TOKEN_EXPIRATION_DAYS));
        pageEntity.setTokenExpiresInDays(60);

        PageEntity saved = pageRepository.save(pageEntity);
        log.info("Facebook page entity saved: {} ({})", saved.getName(), saved.getId());

        return saved;
    }

    // updates token metadata in the DB after a successful refresh. the actual Vault read/write
    // and Graph API call happen in TokenRefreshService before this is called.
    public boolean updateTokenMetadata(String pageId) {
        log.debug("Refreshing token for page: {}", pageId);

        Optional<PageEntity> pageOpt = pageRepository.findById(pageId);
        if (pageOpt.isEmpty()) {
            log.warn("Page not found for token refresh: {}", pageId);
            return false;
        }

        try {
            PageEntity page = pageOpt.get();
            page.setTokenStatus("valid");
            page.setTokenRefreshedAt(LocalDateTime.now());
            page.setLastRefreshSuccess(true);
            page.setLastRefreshError(null);
            page.setLastRefreshAttempt(LocalDateTime.now());
            page.setTokenExpiresAt(LocalDateTime.now().plusDays(TokenConstants.TOKEN_EXPIRATION_DAYS)); // refresh adds another ~60 days
            page.setTokenExpiresInDays(60);

            pageRepository.save(page);
            log.info("Token metadata updated for page: {}", pageId);
            return true;

        } catch (Exception e) {
            log.error("Error refreshing token for page: {}", pageId, e);
            logRefreshFailure(pageId, e.getMessage());
            return false;
        }
    }
}
