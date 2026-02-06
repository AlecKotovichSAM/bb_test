package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.EventGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventGuestRepository extends JpaRepository<EventGuest, Long> {
    List<EventGuest> findByEventId(Long eventId);
}
