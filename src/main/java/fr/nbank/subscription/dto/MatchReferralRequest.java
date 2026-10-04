package fr.nbank.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchReferralRequest(
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(max = 160) String referredUserId,
    @Size(max = 80) String workflowRuntimeId
) {}
