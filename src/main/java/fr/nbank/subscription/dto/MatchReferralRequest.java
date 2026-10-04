package fr.nbank.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchReferralRequest(
    /** Preferred: code from shared install link. */
    @Size(max = 32) String referralCode,
    /** Optional fallback / fill invited email after signup. */
    @Email @Size(max = 160) String email,
    @NotBlank @Size(max = 160) String referredUserId,
    @Size(max = 80) String workflowRuntimeId
) {}
