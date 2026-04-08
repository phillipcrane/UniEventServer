package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecretRepository extends JpaRepository<SecretEntity, Long> {

    Optional<SecretEntity> findByName(String name);

    List<SecretEntity> findBySecretTypeOrderByNameAsc(String secretType);

    List<SecretEntity> findByStatusOrderByNameAsc(String status);
}
