package fr.nbank.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record CreateProductSubscriptionRequest(
    @NotBlank @Pattern(regexp = "^(CARD|OFFER|BANK_PLAN)$") String productType,
    @NotBlank @Size(max = 80) String productId,
    @NotBlank @Size(max = 160) String productName,
    @Size(max = 16) String tenantId,
    @Size(max = 40) String planCode,
    @Size(max = 100) String planName,
    @Pattern(regexp = "^(PENDING|ACTIVE|COMPLETED)$") String status,
    @DecimalMin("0") BigDecimal amount,
    @DecimalMin("0") BigDecimal monthlyFee,
    @NotBlank @Size(max = 8) String currency,
    @Size(max = 80) String externalReference,
    @Pattern(regexp = "^(en|fr|ar)$") String language,
    Map<String, Object> details
) {}
