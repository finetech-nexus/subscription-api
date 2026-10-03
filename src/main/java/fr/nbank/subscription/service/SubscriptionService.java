package fr.nbank.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.nbank.subscription.dto.CreateProductSubscriptionRequest;
import fr.nbank.subscription.dto.CreateSubscriptionRequest;
import fr.nbank.subscription.dto.QuoteOption;
import fr.nbank.subscription.dto.SubscriptionResponse;
import fr.nbank.subscription.entity.AutoQuote;
import fr.nbank.subscription.entity.CustomerSubscription;
import fr.nbank.subscription.repository.CustomerSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {
  private final CustomerSubscriptionRepository subscriptions;
  private final AutoQuoteService quoteService;
  private final ObjectMapper objectMapper;

  public SubscriptionService(CustomerSubscriptionRepository subscriptions, AutoQuoteService quoteService, ObjectMapper objectMapper) {
    this.subscriptions=subscriptions; this.quoteService=quoteService; this.objectMapper=objectMapper;
  }

  @Transactional
  public SubscriptionResponse create(String ownerId, CreateSubscriptionRequest request) {
    AutoQuote quote = quoteService.requireAvailable(ownerId, request.quoteId());
    QuoteOption option = quoteService.readOptions(quote.getOptionsJson()).stream()
        .filter(item -> item.code().equals(request.planCode())).findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is not part of this quotation"));
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    CustomerSubscription saved = subscriptions.save(new CustomerSubscription(id, ownerId, "AUTO_INSURANCE", quote.getId(),
        option.code(), option.name(), option.annualPremium(), option.currency(), "REQUESTED",
        language(request.language()), quote.getDetailsJson(), now, now));
    quoteService.markConverted(quote);
    return response(saved);
  }

  @Transactional
  public SubscriptionResponse createProduct(String ownerId, CreateProductSubscriptionRequest request) {
    String externalReference = blankToNull(request.externalReference());
    Instant now = Instant.now();
    String status = request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status();
    if (externalReference != null) {
      var existing = subscriptions.findFirstByOwnerIdAndProductTypeAndExternalReference(ownerId, request.productType(), externalReference);
      if (existing.isPresent()) {
        CustomerSubscription row = existing.get();
        boolean bankPlan = "BANK_PLAN".equals(request.productType());
        if (!bankPlan || "ACTIVE".equals(row.getStatus())) {
          return response(row);
        }
        // Re-activate a previously cancelled bank plan for the same tier.
        if (bankPlan && "ACTIVE".equals(status)) {
          cancelActiveBankPlans(ownerId, blankToNull(request.tenantId()), now);
          row.setStatus("ACTIVE");
          row.setPlanCode(blankToNull(request.planCode()));
          row.setPlanName(blankToNull(request.planName()));
          row.setProduct(request.productId().trim(), request.productName().trim(), blankToNull(request.tenantId()));
          row.setPricing(request.amount(), request.monthlyFee());
          row.setDetailsJson(writeDetails(request.details()));
          row.setUpdatedAt(now);
          return response(subscriptions.save(row));
        }
      }
    }
    if ("BANK_PLAN".equals(request.productType()) && "ACTIVE".equals(status)) {
      cancelActiveBankPlans(ownerId, blankToNull(request.tenantId()), now);
    }
    CustomerSubscription row = new CustomerSubscription(UUID.randomUUID(), ownerId, request.productType(), null,
        blankToNull(request.planCode()), blankToNull(request.planName()), null,
        request.currency().trim().toUpperCase(Locale.ROOT), status, language(request.language()),
        writeDetails(request.details()), now, now);
    row.setProduct(request.productId().trim(), request.productName().trim(), blankToNull(request.tenantId()));
    row.setPricing(request.amount(), request.monthlyFee());
    row.setExternalReference(externalReference);
    return response(subscriptions.save(row));
  }

  @Transactional(readOnly = true)
  public List<SubscriptionResponse> list(String ownerId) {
    return subscriptions.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::response).toList();
  }

  @Transactional(readOnly = true)
  public SubscriptionResponse get(String ownerId, UUID id) {
    return response(findOwned(ownerId, id));
  }

  @Transactional
  public SubscriptionResponse cancel(String ownerId, UUID id) {
    CustomerSubscription subscription = findOwned(ownerId, id);
    if ("CANCELLED".equals(subscription.getStatus())) return response(subscription);
    boolean bankPlan = "BANK_PLAN".equals(subscription.getProductType());
    if (!"REQUESTED".equals(subscription.getStatus()) && !(bankPlan && "ACTIVE".equals(subscription.getStatus()))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending subscription requests can be cancelled");
    }
    subscription.setStatus("CANCELLED");
    subscription.setUpdatedAt(Instant.now());
    return response(subscriptions.save(subscription));
  }

  @Transactional(readOnly = true)
  public SubscriptionResponse currentBankPlan(String ownerId, String tenantId) {
    String tenant = blankToNull(tenantId);
    if (tenant != null) {
      return subscriptions
          .findFirstByOwnerIdAndProductTypeAndTenantIdAndStatusOrderByCreatedAtDesc(ownerId, "BANK_PLAN", tenant, "ACTIVE")
          .map(this::response)
          .orElse(null);
    }
    return subscriptions
        .findByOwnerIdAndProductTypeAndStatusOrderByCreatedAtDesc(ownerId, "BANK_PLAN", "ACTIVE")
        .stream()
        .findFirst()
        .map(this::response)
        .orElse(null);
  }

  private void cancelActiveBankPlans(String ownerId, String tenantId, Instant now) {
    List<CustomerSubscription> active = subscriptions
        .findByOwnerIdAndProductTypeAndStatusOrderByCreatedAtDesc(ownerId, "BANK_PLAN", "ACTIVE");
    for (CustomerSubscription row : active) {
      if (tenantId != null && row.getTenantId() != null && !tenantId.equalsIgnoreCase(row.getTenantId())) {
        continue;
      }
      row.setStatus("CANCELLED");
      row.setUpdatedAt(now);
      subscriptions.save(row);
    }
  }

  private CustomerSubscription findOwned(String ownerId, UUID id) {
    return subscriptions.findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
  }

  private SubscriptionResponse response(CustomerSubscription row) {
    return new SubscriptionResponse(row.getId(), "SUB-" + row.getId().toString().substring(0, 8).toUpperCase(),
        row.getProductType(), row.getProductId(), row.getProductName(), row.getTenantId(), language(row.getLanguage()),
        row.getQuoteId(), row.getPlanCode(), row.getPlanName(), row.getAnnualPremium(), row.getAmount(), row.getMonthlyFee(),
        row.getCurrency(), row.getStatus(), row.getExternalReference(), message(row.getStatus()), readDetails(row.getDetailsJson()),
        row.getCreatedAt(), row.getUpdatedAt());
  }

  private String writeDetails(Map<String, Object> details) {
    try {
      return objectMapper.writeValueAsString(details == null ? Map.of() : details);
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription details are invalid");
    }
  }

  private Map<String, Object> readDetails(String json) {
    if (json == null || json.isBlank()) return Map.of();
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException ex) {
      return Map.of();
    }
  }

  private static String language(String language) {
    return language == null || language.isBlank() ? "en" : language;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String message(String status) {
    return switch (status) {
      case "REQUESTED" -> "Votre demande de souscription a été enregistrée.";
      case "PENDING" -> "Votre souscription est en cours de traitement.";
      case "ACTIVE" -> "Votre souscription est active.";
      case "COMPLETED" -> "Votre opération a été effectuée.";
      case "CANCELLED" -> "Votre demande a été annulée.";
      default -> "Souscription mise à jour.";
    };
  }
}
