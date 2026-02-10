package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.EventGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EventGuestRepository extends JpaRepository<EventGuest, Long> {
    List<EventGuest> findByEventId(Long eventId);
    
    // Поиск первого EventGuest по guest_id (ID из таблицы guests) для копирования детей
    // Используем JPQL с JOIN для поиска по связанной сущности Guest
    // НЕ используем FETCH, чтобы избежать проблем с загрузкой связи
    @Query("SELECT eg FROM EventGuest eg JOIN eg.guest g WHERE g.id = :guestId ORDER BY eg.id ASC")
    List<EventGuest> findByGuestIdOrderByIdAsc(@Param("guestId") Long guestId);
    
    // Поиск первого EventGuest по guest_id из событий конкретного пользователя (hostId)
    // Это гарантирует, что мы найдем EventGuest из событий того же пользователя, а не из всех событий
    @Query("SELECT eg FROM EventGuest eg JOIN eg.guest g WHERE g.id = :guestId AND eg.eventId IN (SELECT e.id FROM eu.bb.app.backend.entity.Event e WHERE e.hostId = :hostId) ORDER BY eg.id ASC")
    List<EventGuest> findByGuestIdAndHostIdOrderByIdAsc(@Param("guestId") Long guestId, @Param("hostId") Long hostId);
    
    // Вспомогательный метод для получения первого EventGuest из событий конкретного пользователя
    default Optional<EventGuest> findFirstByGuestIdAndHostId(Long guestId, Long hostId) {
        List<EventGuest> results = findByGuestIdAndHostIdOrderByIdAsc(guestId, hostId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    // Вспомогательный метод для получения первого EventGuest (из всех событий, для обратной совместимости)
    default Optional<EventGuest> findFirstByGuestId(Long guestId) {
        List<EventGuest> results = findByGuestIdOrderByIdAsc(guestId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
