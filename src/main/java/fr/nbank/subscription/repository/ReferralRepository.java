package fr.nbank.subscription.repository;

import fr.nbank.subscription.entity.Referral;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
  List<Referral> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

  Optional<Referral> findFirstByInvitedEmailIgnoreCaseAndStatusInOrderByCreatedAtAsc(
      String invitedEmail, List<String> statuses);

  Optional<Referral> findFirstByWorkflowRuntimeId(String workflowRuntimeId);

  boolean existsByOwnerIdAndInvitedEmailIgnoreCaseAndStatusIn(String ownerId, String invitedEmail, List<String> statuses);
}
