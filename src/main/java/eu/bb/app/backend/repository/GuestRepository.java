package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    // Найти гостя по имени и userId (для поиска существующего гостя)
    Optional<Guest> findByGuestNameAndUserId(String guestName, Long userId);
    
    // Найти гостей по userId
    List<Guest> findByUserId(Long userId);
    
    // Найти гостей по имени (для незарегистрированных)
    List<Guest> findByGuestName(String guestName);
}
