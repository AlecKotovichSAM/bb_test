package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {
    Optional<UserAvatar> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
