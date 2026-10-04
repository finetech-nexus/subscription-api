package fr.nbank.subscription.repository;

import fr.nbank.subscription.entity.Referral;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
  List<Referral> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(String ownerId, String status);

  Optional<Referral> findFirstByOwnerIdAndStatusOrderByCreatedAtDesc(String ownerId, String status);

  Optional<Referral> findFirstByReferralCodeIgnoreCaseAndStatusIn(String referralCode, List<String> statuses);

  Optional<Referral> findFirstByInvitedEmailIgnoreCaseAndStatusInOrderByCreatedAtAsc(
      String invitedEmail, List<String> statuses);

  Optional<Referral> findFirstByWorkflowRuntimeId(String workflowRuntimeId);

  boolean existsByReferralCodeIgnoreCase(String referralCode);
}
