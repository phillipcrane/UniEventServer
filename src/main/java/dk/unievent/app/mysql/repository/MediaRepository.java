package dk.unievent.app.mysql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.unievent.app.mysql.model.MediaEntity;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
}
