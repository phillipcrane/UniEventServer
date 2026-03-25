package dk.unievent.app.mysql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import dk.unievent.app.mysql.model.PageEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, String> {
    
    /**
     * Find all pages ordered by name
     */
    List<PageEntity> findAllByOrderByNameAsc();
    
    /**
     * Find all active pages (tokenStatus = "valid")
     */
    List<PageEntity> findByTokenStatusOrderByNameAsc(String tokenStatus);
    
    /**
     * Find pages with expired tokens (tokenExpiresAt < now)
     */
    List<PageEntity> findByTokenExpiresAtLessThanOrderByTokenExpiresAtAsc(LocalDateTime expiresAt);
    
    /**
     * Find pages that need token refresh (tokenExpiresAt < now AND tokenStatus = "valid")
     */
    @Query("SELECT p FROM PageEntity p WHERE p.tokenExpiresAt < ?1 AND p.tokenStatus = 'valid' ORDER BY p.tokenExpiresAt ASC")
    List<PageEntity> findPagesToRefresh(LocalDateTime expiresAt);
    
    /**
     * Find a page by name (case-insensitive)
     */
    List<PageEntity> findByNameIgnoreCase(String name);
}
