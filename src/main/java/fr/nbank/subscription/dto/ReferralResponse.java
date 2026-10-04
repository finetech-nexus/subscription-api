package fr.nbank.subscription.dto;

import fr.nbank.subscription.entity.Referral;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReferralResponse(
    UUID id,
    String ownerId,
    String referralCode,
    String shareUrl,
    String invitedEmail,
    String status,
    BigDecimal rewardAmount,
    String currency,
    String tenantId,
    String accountId,
    String campaignCode,
    String referredUserId,
    String workflowRuntimeId,
    Instant sharedAt,
    Instant creditedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static ReferralResponse from(Referral row, String shareBaseUrl) {
    String code = row.getReferralCode();
    String shareUrl = null;
    if (code != null && !code.isBlank() && shareBaseUrl != null && !shareBaseUrl.isBlank()) {
      String base = shareBaseUrl.endsWith("/") ? shareBaseUrl.substring(0, shareBaseUrl.length() - 1) : shareBaseUrl;
      shareUrl = base + "/" + code;
    }
    return new ReferralResponse(
        row.getId(),
        row.getOwnerId(),
        code,
        shareUrl,
        row.getInvitedEmail(),
        row.getStatus(),
        row.getRewardAmount(),
        row.getCurrency(),
        row.getTenantId(),
        row.getAccountId(),
        row.getCampaignCode(),
        row.getReferredUserId(),
        row.getWorkflowRuntimeId(),
        row.getSharedAt(),
        row.getCreditedAt(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }
}
