package fr.nbank.subscription.repository;

import fr.nbank.subscription.entity.AutoQuote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoQuoteRepository extends JpaRepository<AutoQuote, UUID> {
  Optional<AutoQuote> findByIdAndOwnerId(UUID id, String ownerId);
}
