package dk.unievent.app.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.unievent.app.db.model.MediaEntity;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
}
