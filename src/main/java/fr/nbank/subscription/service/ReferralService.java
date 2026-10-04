package fr.nbank.subscription.service;

import fr.nbank.subscription.dto.CreateReferralRequest;
import fr.nbank.subscription.dto.MatchReferralRequest;
import fr.nbank.subscription.dto.ReferralResponse;
import fr.nbank.subscription.entity.Referral;
import fr.nbank.subscription.repository.ReferralRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReferralService {
  public static final String PENDING = "PENDING";
  public static final String IN_REVIEW = "IN_REVIEW";
  public static final String APPROVED = "APPROVED";
  public static final String CREDITED = "CREDITED";
  public static final String REJECTED = "REJECTED";

  private static final List<String> OPEN = List.of(PENDING, IN_REVIEW);

  private final ReferralRepository referrals;

  public ReferralService(ReferralRepository referrals) {
    this.referrals = referrals;
  }

  @Transactional
  public ReferralResponse invite(String ownerId, CreateReferralRequest request) {
    String email = request.invitedEmail().trim().toLowerCase(Locale.ROOT);
    if (referrals.existsByOwnerIdAndInvitedEmailIgnoreCaseAndStatusIn(ownerId, email, OPEN)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "An open invitation already exists for this email");
    }
    Instant now = Instant.now();
    BigDecimal reward = request.rewardAmount() != null ? request.rewardAmount() : BigDecimal.ZERO;
    String currency = request.currency() != null && !request.currency().isBlank()
        ? request.currency().trim().toUpperCase(Locale.ROOT)
        : "EUR";
    Referral row = new Referral(
        UUID.randomUUID(),
        ownerId,
        blank(request.tenantId()),
        blank(request.accountId()),
        email,
        PENDING,
        reward,
        currency,
        blank(request.campaignCode()) != null ? blank(request.campaignCode()) : "PARRAINAGE",
        now,
        now);
    return ReferralResponse.from(referrals.save(row));
  }

  @Transactional(readOnly = true)
  public List<ReferralResponse> list(String ownerId) {
    return referrals.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(ReferralResponse::from).toList();
  }

  /** Called when the invited email creates an account (KYC onboard). Moves PENDING → IN_REVIEW. */
  @Transactional
  public ReferralResponse match(MatchReferralRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    Referral row = referrals
        .findFirstByInvitedEmailIgnoreCaseAndStatusInOrderByCreatedAtAsc(email, List.of(PENDING))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending referral for this email"));
    Instant now = Instant.now();
    row.setStatus(IN_REVIEW);
    row.setReferredUserId(request.referredUserId().trim());
    if (request.workflowRuntimeId() != null && !request.workflowRuntimeId().isBlank()) {
      row.setWorkflowRuntimeId(request.workflowRuntimeId().trim());
    }
    row.setUpdatedAt(now);
    return ReferralResponse.from(referrals.save(row));
  }

  @Transactional
  public ReferralResponse attachWorkflow(UUID id, String workflowRuntimeId) {
    Referral row = referrals.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found"));
    row.setWorkflowRuntimeId(workflowRuntimeId);
    row.setStatus(IN_REVIEW);
    row.setUpdatedAt(Instant.now());
    return ReferralResponse.from(referrals.save(row));
  }

  @Transactional
  public ReferralResponse approveByWorkflow(String workflowRuntimeId) {
    Referral row = referrals.findFirstByWorkflowRuntimeId(workflowRuntimeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found for workflow"));
    if (CREDITED.equals(row.getStatus()) || REJECTED.equals(row.getStatus())) {
      return ReferralResponse.from(row);
    }
    row.setStatus(APPROVED);
    row.setUpdatedAt(Instant.now());
    return ReferralResponse.from(referrals.save(row));
  }

  @Transactional
  public ReferralResponse markCredited(UUID id) {
    Referral row = referrals.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found"));
    Instant now = Instant.now();
    row.setStatus(CREDITED);
    row.setCreditedAt(now);
    row.setUpdatedAt(now);
    return ReferralResponse.from(referrals.save(row));
  }

  @Transactional
  public ReferralResponse rejectByWorkflow(String workflowRuntimeId) {
    Referral row = referrals.findFirstByWorkflowRuntimeId(workflowRuntimeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found for workflow"));
    row.setStatus(REJECTED);
    row.setUpdatedAt(Instant.now());
    return ReferralResponse.from(referrals.save(row));
  }

  private static String blank(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
