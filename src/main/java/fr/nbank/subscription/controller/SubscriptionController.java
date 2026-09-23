package fr.nbank.subscription.controller;

import fr.nbank.subscription.dto.CreateSubscriptionRequest;
import fr.nbank.subscription.dto.SubscriptionResponse;
import fr.nbank.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Customer subscriptions", description = "Create and manage your insurance subscription requests")
public class SubscriptionController {
  private final SubscriptionService service;
  private final boolean authDisabled;

  public SubscriptionController(SubscriptionService service, @Value("${app.security.disabled:false}") boolean authDisabled) {
    this.service=service; this.authDisabled=authDisabled;
  }

  @PostMapping
  @Operation(summary = "Create a subscription request from a quotation")
  public ResponseEntity<SubscriptionResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateSubscriptionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ownerId(jwt), request));
  }

  @GetMapping
  @Operation(summary = "List your subscriptions")
  public List<SubscriptionResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(ownerId(jwt));
  }

  @GetMapping("/{subscriptionId}")
  @Operation(summary = "Read one of your subscriptions")
  public SubscriptionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID subscriptionId) {
    return service.get(ownerId(jwt), subscriptionId);
  }

  @PostMapping("/{subscriptionId}/cancel")
  @Operation(summary = "Cancel a pending subscription request")
  public SubscriptionResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID subscriptionId) {
    return service.cancel(ownerId(jwt), subscriptionId);
  }

  private String ownerId(Jwt jwt) {
    if (jwt != null && !jwt.getSubject().isBlank()) return jwt.getSubject();
    if (authDisabled) return "local-demo";
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
  }
}
