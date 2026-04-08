package dk.unievent.app.application.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.mapper.PageMapper;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<PageDTO> getAllPages() {
        return pageRepository.findAllByOrderByNameAsc()
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }

    public PageDTO getPageById(String id) {
        Optional<PageEntity> entity = pageRepository.findById(id);
        return entity.map(pageMapper::toDTO).orElse(null);
    }

    public List<PageDTO> getActivePages() {
        return pageRepository.findByTokenStatusOrderByNameAsc("valid")
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<PageEntity> getPagesToRefresh() {
        return pageRepository.findPagesToRefresh(LocalDateTime.now());
    }

    public List<PageDTO> getExpiredPages() {
        return pageRepository.findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(LocalDateTime.now())
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<PageDTO> searchPagesByName(String name) {
        return pageRepository.findByNameIgnoreCase(name)
            .stream()
            .map(pageMapper::toDTO)
            .collect(Collectors.toList());
    }

    public PageDTO savePage(PageDTO pageDTO) {
        PageEntity entity = pageMapper.toEntity(pageDTO);

        if (pageDTO.getPictureId() != null) {
            MediaEntity picture = mediaRepository.findById(pageDTO.getPictureId()).orElse(null);
            entity.setPicture(picture);
        }

        PageEntity saved = pageRepository.save(entity);
        return pageMapper.toDTO(saved);
    }

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

    public PageDTO uploadPicture(String id, MultipartFile file) throws IOException {
        Optional<PageEntity> existing = pageRepository.findById(id);
        if (existing.isEmpty()) {
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

        return pageMapper.toDTO(updated);
    }

    public boolean deletePage(String id) {
        if (pageRepository.existsById(id)) {
            pageRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
