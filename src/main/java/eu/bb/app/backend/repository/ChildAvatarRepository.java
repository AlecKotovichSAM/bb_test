package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.ChildAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChildAvatarRepository extends JpaRepository<ChildAvatar, Long> {
    Optional<ChildAvatar> findByChildId(Long childId);
    void deleteByChildId(Long childId);
}
