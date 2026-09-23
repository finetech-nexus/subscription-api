package fr.nbank.subscription.controller;

import fr.nbank.subscription.dto.AutoQuoteRequest;
import fr.nbank.subscription.dto.AutoQuoteResponse;
import fr.nbank.subscription.service.AutoQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insurance/auto/quotes")
@Tag(name = "Auto insurance quotes", description = "Mocked auto insurance quotation flow")
public class AutoQuoteController {
  private final AutoQuoteService service;
  private final boolean authDisabled;

  public AutoQuoteController(AutoQuoteService service, @Value("${app.security.disabled:false}") boolean authDisabled) {
    this.service=service; this.authDisabled=authDisabled;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Get a mocked auto insurance quote", description = "Stores an expiring quote for the authenticated customer and returns selectable package estimates.")
  public AutoQuoteResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AutoQuoteRequest request) {
    return service.create(ownerId(jwt), request);
  }

  @GetMapping("/{quoteId}")
  @Operation(summary = "Read one of your quotations")
  public AutoQuoteResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID quoteId) {
    return service.get(ownerId(jwt), quoteId);
  }

  private String ownerId(Jwt jwt) {
    if (jwt != null && !jwt.getSubject().isBlank()) return jwt.getSubject();
    if (authDisabled) return "local-demo";
    throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
  }
}
