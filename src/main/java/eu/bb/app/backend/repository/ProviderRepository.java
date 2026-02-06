package eu.bb.app.backend.repository;

import eu.bb.app.backend.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, Long> {}
