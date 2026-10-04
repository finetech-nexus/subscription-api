package fr.nbank.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_referral", indexes = {
    @Index(name = "idx_referral_owner_created", columnList = "owner_id,created_at"),
    @Index(name = "idx_referral_email_status", columnList = "invited_email,status"),
    @Index(name = "idx_referral_workflow", columnList = "workflow_runtime_id")
})
public class Referral {
  @Id private UUID id;
  @Column(name = "owner_id", nullable = false, length = 160) private String ownerId;
  @Column(name = "tenant_id", length = 16) private String tenantId;
  @Column(name = "account_id", length = 40) private String accountId;
  @Column(name = "invited_email", nullable = false, length = 160) private String invitedEmail;
  @Column(nullable = false, length = 24) private String status;
  @Column(name = "reward_amount", precision = 12, scale = 2) private BigDecimal rewardAmount;
  @Column(nullable = false, length = 8) private String currency;
  @Column(name = "campaign_code", length = 40) private String campaignCode;
  @Column(name = "referred_user_id", length = 160) private String referredUserId;
  @Column(name = "workflow_runtime_id", length = 80) private String workflowRuntimeId;
  @Column(name = "credited_at") private Instant creditedAt;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  protected Referral() {}

  public Referral(UUID id, String ownerId, String tenantId, String accountId, String invitedEmail, String status,
      BigDecimal rewardAmount, String currency, String campaignCode, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.tenantId = tenantId;
    this.accountId = accountId;
    this.invitedEmail = invitedEmail;
    this.status = status;
    this.rewardAmount = rewardAmount;
    this.currency = currency;
    this.campaignCode = campaignCode;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() { return id; }
  public String getOwnerId() { return ownerId; }
  public String getTenantId() { return tenantId; }
  public String getAccountId() { return accountId; }
  public String getInvitedEmail() { return invitedEmail; }
  public String getStatus() { return status; }
  public BigDecimal getRewardAmount() { return rewardAmount; }
  public String getCurrency() { return currency; }
  public String getCampaignCode() { return campaignCode; }
  public String getReferredUserId() { return referredUserId; }
  public String getWorkflowRuntimeId() { return workflowRuntimeId; }
  public Instant getCreditedAt() { return creditedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setStatus(String status) { this.status = status; }
  public void setReferredUserId(String referredUserId) { this.referredUserId = referredUserId; }
  public void setWorkflowRuntimeId(String workflowRuntimeId) { this.workflowRuntimeId = workflowRuntimeId; }
  public void setCreditedAt(Instant creditedAt) { this.creditedAt = creditedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public void setAccountId(String accountId) { this.accountId = accountId; }
}
