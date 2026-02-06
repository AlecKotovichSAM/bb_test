package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByEventIdOrderByCreatedAtAsc(Long eventId);
}
