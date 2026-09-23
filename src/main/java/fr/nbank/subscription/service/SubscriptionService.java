package fr.nbank.subscription.service;

import fr.nbank.subscription.dto.CreateSubscriptionRequest;
import fr.nbank.subscription.dto.QuoteOption;
import fr.nbank.subscription.dto.SubscriptionResponse;
import fr.nbank.subscription.entity.AutoQuote;
import fr.nbank.subscription.entity.CustomerSubscription;
import fr.nbank.subscription.repository.CustomerSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {
  private final CustomerSubscriptionRepository subscriptions;
  private final AutoQuoteService quoteService;

  public SubscriptionService(CustomerSubscriptionRepository subscriptions, AutoQuoteService quoteService) {
    this.subscriptions=subscriptions; this.quoteService=quoteService;
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
        option.code(), option.name(), option.annualPremium(), option.currency(), "REQUESTED", quote.getDetailsJson(), now, now));
    quoteService.markConverted(quote);
    return response(saved);
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
    if (!"REQUESTED".equals(subscription.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending subscription requests can be cancelled");
    }
    subscription.setStatus("CANCELLED");
    subscription.setUpdatedAt(Instant.now());
    return response(subscriptions.save(subscription));
  }

  private CustomerSubscription findOwned(String ownerId, UUID id) {
    return subscriptions.findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
  }

  private SubscriptionResponse response(CustomerSubscription row) {
    return new SubscriptionResponse(row.getId(), "SUB-" + row.getId().toString().substring(0, 8).toUpperCase(),
        row.getProductType(), row.getQuoteId(), row.getPlanCode(), row.getPlanName(), row.getAnnualPremium(),
        row.getCurrency(), row.getStatus(), message(row.getStatus()), row.getCreatedAt(), row.getUpdatedAt());
  }

  private static String message(String status) {
    return switch (status) {
      case "REQUESTED" -> "Votre demande de souscription a été enregistrée.";
      case "CANCELLED" -> "Votre demande a été annulée.";
      default -> "Souscription mise à jour.";
    };
  }
}
