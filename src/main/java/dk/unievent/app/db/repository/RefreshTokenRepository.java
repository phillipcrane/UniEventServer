package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenId(String tokenId);

    List<RefreshTokenEntity> findAllByFamilyIdAndRevokedAtIsNull(String familyId);

    List<RefreshTokenEntity> findAllByUserEmailAndRevokedAtIsNull(String userEmail);

    List<RefreshTokenEntity> findAllByExpiresAtBefore(Instant now);
}