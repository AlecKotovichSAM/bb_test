package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findByEventId(Long eventId);
    
    // Загрузка подарка с категориями
    @Query("SELECT DISTINCT g FROM Gift g LEFT JOIN FETCH g.categories WHERE g.id = :id")
    Optional<Gift> findByIdWithCategories(@Param("id") Long id);
    
    // Загрузка подарков события с категориями
    @Query("SELECT DISTINCT g FROM Gift g LEFT JOIN FETCH g.categories WHERE g.eventId = :eventId")
    List<Gift> findByEventIdWithCategories(@Param("eventId") Long eventId);
}
