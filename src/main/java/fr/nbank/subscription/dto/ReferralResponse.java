package fr.nbank.subscription.dto;

import fr.nbank.subscription.entity.Referral;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReferralResponse(
    UUID id,
    String ownerId,
    String invitedEmail,
    String status,
    BigDecimal rewardAmount,
    String currency,
    String tenantId,
    String accountId,
    String campaignCode,
    String referredUserId,
    String workflowRuntimeId,
    Instant creditedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static ReferralResponse from(Referral row) {
    return new ReferralResponse(
        row.getId(),
        row.getOwnerId(),
        row.getInvitedEmail(),
        row.getStatus(),
        row.getRewardAmount(),
        row.getCurrency(),
        row.getTenantId(),
        row.getAccountId(),
        row.getCampaignCode(),
        row.getReferredUserId(),
        row.getWorkflowRuntimeId(),
        row.getCreditedAt(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }
}
