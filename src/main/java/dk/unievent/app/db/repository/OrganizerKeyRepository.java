package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.OrganizerKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizerKeyRepository extends JpaRepository<OrganizerKeyEntity, Long> {

    Optional<OrganizerKeyEntity> findByKeyValue(String keyValue);

    List<OrganizerKeyEntity> findByEmailAndUsedAtIsNull(String email);

    List<OrganizerKeyEntity> findByExpiresAtBeforeAndUsedAtIsNull(Instant expiresAt);
}
