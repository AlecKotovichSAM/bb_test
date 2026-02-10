package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.GiftCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GiftCategoryRepository extends JpaRepository<GiftCategory, Long> {
    List<GiftCategory> findAllByOrderByNameAsc();
    Optional<GiftCategory> findByName(String name);
}
