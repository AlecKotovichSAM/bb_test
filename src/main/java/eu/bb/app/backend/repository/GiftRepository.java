package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findByEventId(Long eventId);
}
