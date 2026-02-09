package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.GuestToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuestTokenRepository extends JpaRepository<GuestToken, Long> {
    Optional<GuestToken> findByToken(String token);
    List<GuestToken> findByGuestId(Long guestId);
}
