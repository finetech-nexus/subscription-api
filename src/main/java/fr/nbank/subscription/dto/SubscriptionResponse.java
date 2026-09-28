package fr.nbank.subscription.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    String reference,
    String productType,
    String productId,
    String productName,
    String tenantId,
    String language,
    UUID quoteId,
    String planCode,
    String planName,
    BigDecimal annualPremium,
    BigDecimal amount,
    BigDecimal monthlyFee,
    String currency,
    String status,
    String externalReference,
    String message,
    Map<String, Object> details,
    Instant createdAt,
    Instant updatedAt
) {}
