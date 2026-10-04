package fr.nbank.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Create / prepare a shareable parrainage code (no email). */
public record CreateReferralRequest(
    @Size(max = 16) String tenantId,
    @Size(max = 40) String accountId,
    @DecimalMin("0") BigDecimal rewardAmount,
    @Size(max = 8) String currency,
    @Size(max = 40) String campaignCode
) {}
