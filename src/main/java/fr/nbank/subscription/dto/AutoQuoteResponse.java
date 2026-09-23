package fr.nbank.subscription.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AutoQuoteResponse(
    UUID quoteId,
    String status,
    boolean mock,
    Instant expiresAt,
    String registrationNumber,
    String vehicleDescription,
    String insuredName,
    List<QuoteOption> options
) {}
