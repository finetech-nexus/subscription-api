package fr.nbank.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CreateSubscriptionRequest(
    @NotNull UUID quoteId,
    @NotBlank String planCode,
    @AssertTrue(message = "Terms must be accepted") boolean termsAccepted,
    @Pattern(regexp = "^(en|fr|ar)$") String language
) {}
