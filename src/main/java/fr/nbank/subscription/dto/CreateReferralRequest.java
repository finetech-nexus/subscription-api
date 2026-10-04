package fr.nbank.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateReferralRequest(
    @NotBlank @Email @Size(max = 160) String invitedEmail,
    @Size(max = 16) String tenantId,
    @Size(max = 40) String accountId,
    @DecimalMin("0") BigDecimal rewardAmount,
    @Size(max = 8) String currency,
    @Size(max = 40) String campaignCode
) {}
