package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findByUserId(Long userId);
}
