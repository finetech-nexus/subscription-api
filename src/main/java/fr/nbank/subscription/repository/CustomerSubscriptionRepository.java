package fr.nbank.subscription.repository;

import fr.nbank.subscription.entity.CustomerSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSubscriptionRepository extends JpaRepository<CustomerSubscription, UUID> {
  List<CustomerSubscription> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
  Optional<CustomerSubscription> findByIdAndOwnerId(UUID id, String ownerId);
}
