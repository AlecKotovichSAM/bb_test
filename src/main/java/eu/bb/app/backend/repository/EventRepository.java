package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByHostId(Long hostId);
}
