package fr.nbank.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import java.util.UUID;

public record CreateSubscriptionRequest(
    @NotNull UUID quoteId,
    @NotBlank String planCode,
    @AssertTrue(message = "Terms must be accepted") boolean termsAccepted
) {}
