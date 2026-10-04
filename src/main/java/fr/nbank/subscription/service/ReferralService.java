package fr.nbank.subscription.service;

import fr.nbank.subscription.dto.CreateReferralRequest;
import fr.nbank.subscription.dto.MatchReferralRequest;
import fr.nbank.subscription.dto.ReferralResponse;
import fr.nbank.subscription.dto.ShareReferralResponse;
import fr.nbank.subscription.entity.Referral;
import fr.nbank.subscription.repository.ReferralRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReferralService {
  /** Generated, not yet shared — current code shown on the invite screen. */
  public static final String READY = "READY";
  /** Shared via the share button — waiting for friend signup. */
  public static final String PENDING = "PENDING";
  public static final String IN_REVIEW = "IN_REVIEW";
  public static final String APPROVED = "APPROVED";
  public static final String CREDITED = "CREDITED";
  public static final String REJECTED = "REJECTED";

  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ReferralRepository referrals;
  private final String shareBaseUrl;

  public ReferralService(
      ReferralRepository referrals,
      @Value("${app.parrainage.share-base-url:https://app.nexus-bank.io/r}") String shareBaseUrl) {
    this.referrals = referrals;
    this.shareBaseUrl = shareBaseUrl == null ? "" : shareBaseUrl.trim();
  }

  /** Current ready-to-share code (creates one if missing). */
  @Transactional
  public ReferralResponse current(String ownerId, CreateReferralRequest request) {
    return toResponse(ensureReady(ownerId, request));
  }

  /**
   * Share current code: mark READY → PENDING, generate a new READY code.
   * Returns the shared row and the next code.
   */
  @Transactional
  public ShareReferralResponse share(String ownerId, CreateReferralRequest request) {
    Referral ready = ensureReady(ownerId, request);
    Instant now = Instant.now();
    ready.setStatus(PENDING);
    ready.setSharedAt(now);
    ready.setUpdatedAt(now);
    applyRequestDefaults(ready, request);
    Referral shared = referrals.save(ready);
    Referral next = createReady(ownerId, request, now);
    return new ShareReferralResponse(toResponse(shared), toResponse(next));
  }

  @Transactional(readOnly = true)
  public List<ReferralResponse> list(String ownerId) {
    return referrals.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(ownerId, READY).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Called when a friend signs up with a shared code (or legacy email match). */
  @Transactional
  public ReferralResponse match(MatchReferralRequest request) {
    String code = blank(request.referralCode());
    String email = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : null;
    if (code == null && (email == null || email.isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referralCode or email is required");
    }

    Referral row = null;
    if (code != null) {
      row = referrals
          .findFirstByReferralCodeIgnoreCaseAndStatusIn(code.toUpperCase(Locale.ROOT), List.of(PENDING))
          .orElse(null);
    }
    if (row == null && email != null && !email.isBlank()) {
      row = referrals
          .findFirstByInvitedEmailIgnoreCaseAndStatusInOrderByCreatedAtAsc(email, List.of(PENDING))
          .orElse(null);
    }
    if (row == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending referral for this code/email");
    }

    Instant now = Instant.now();
    row.setStatus(IN_REVIEW);
    row.setReferredUserId(request.referredUserId().trim());
    if (email != null && !email.isBlank()) {
      row.setInvitedEmail(email);
    }
    if (request.workflowRuntimeId() != null && !request.workflowRuntimeId().isBlank()) {
      row.setWorkflowRuntimeId(request.workflowRuntimeId().trim());
    }
    row.setUpdatedAt(now);
    return toResponse(referrals.save(row));
  }

  @Transactional
  public ReferralResponse attachWorkflow(UUID id, String workflowRuntimeId) {
    Referral row = referrals.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found"));
    row.setWorkflowRuntimeId(workflowRuntimeId);
    row.setStatus(IN_REVIEW);
    row.setUpdatedAt(Instant.now());
    return toResponse(referrals.save(row));
  }

  @Transactional
  public ReferralResponse approveByWorkflow(String workflowRuntimeId) {
    Referral row = referrals.findFirstByWorkflowRuntimeId(workflowRuntimeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found for workflow"));
    if (CREDITED.equals(row.getStatus()) || REJECTED.equals(row.getStatus())) {
      return toResponse(row);
    }
    row.setStatus(APPROVED);
    row.setUpdatedAt(Instant.now());
    return toResponse(referrals.save(row));
  }

  @Transactional
  public ReferralResponse markCredited(UUID id) {
    Referral row = referrals.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found"));
    Instant now = Instant.now();
    row.setStatus(CREDITED);
    row.setCreditedAt(now);
    row.setUpdatedAt(now);
    return toResponse(referrals.save(row));
  }

  @Transactional
  public ReferralResponse rejectByWorkflow(String workflowRuntimeId) {
    Referral row = referrals.findFirstByWorkflowRuntimeId(workflowRuntimeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referral not found for workflow"));
    row.setStatus(REJECTED);
    row.setUpdatedAt(Instant.now());
    return toResponse(referrals.save(row));
  }

  private Referral ensureReady(String ownerId, CreateReferralRequest request) {
    return referrals
        .findFirstByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, READY)
        .map(existing -> {
          applyRequestDefaults(existing, request);
          existing.setUpdatedAt(Instant.now());
          return referrals.save(existing);
        })
        .orElseGet(() -> createReady(ownerId, request, Instant.now()));
  }

  private Referral createReady(String ownerId, CreateReferralRequest request, Instant now) {
    BigDecimal reward = request != null && request.rewardAmount() != null ? request.rewardAmount() : BigDecimal.ZERO;
    String currency = request != null && request.currency() != null && !request.currency().isBlank()
        ? request.currency().trim().toUpperCase(Locale.ROOT)
        : "EUR";
    String campaign = request != null && blank(request.campaignCode()) != null
        ? blank(request.campaignCode())
        : "PARRAINAGE";
    Referral row = new Referral(
        UUID.randomUUID(),
        ownerId,
        request != null ? blank(request.tenantId()) : null,
        request != null ? blank(request.accountId()) : null,
        generateUniqueCode(),
        null,
        READY,
        reward,
        currency,
        campaign,
        now,
        now);
    return referrals.save(row);
  }

  private void applyRequestDefaults(Referral row, CreateReferralRequest request) {
    if (request == null) return;
    if (blank(request.tenantId()) != null) row.setTenantId(blank(request.tenantId()));
    if (blank(request.accountId()) != null) row.setAccountId(blank(request.accountId()));
    if (request.rewardAmount() != null) row.setRewardAmount(request.rewardAmount());
    if (request.currency() != null && !request.currency().isBlank()) {
      row.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
    }
    if (blank(request.campaignCode()) != null) row.setCampaignCode(blank(request.campaignCode()));
  }

  private String generateUniqueCode() {
    for (int attempt = 0; attempt < 20; attempt++) {
      StringBuilder sb = new StringBuilder(CODE_LENGTH);
      for (int i = 0; i < CODE_LENGTH; i++) {
        sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
      }
      String code = sb.toString();
      if (!referrals.existsByReferralCodeIgnoreCase(code)) return code;
    }
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate referral code");
  }

  private ReferralResponse toResponse(Referral row) {
    return ReferralResponse.from(row, shareBaseUrl);
  }

  private static String blank(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
