package fr.nbank.subscription.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    String reference,
    String productType,
    UUID quoteId,
    String planCode,
    String planName,
    BigDecimal annualPremium,
    String currency,
    String status,
    String message,
    Instant createdAt,
    Instant updatedAt
) {}
