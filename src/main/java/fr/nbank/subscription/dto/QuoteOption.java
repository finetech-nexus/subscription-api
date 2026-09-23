package fr.nbank.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuoteOption(
    String code,
    String name,
    String description,
    BigDecimal annualPremium,
    BigDecimal monthlyEstimate,
    String currency,
    List<String> coverages
) {}
