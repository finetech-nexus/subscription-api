package fr.nbank.subscription.controller;

import fr.nbank.subscription.dto.CreateReferralRequest;
import fr.nbank.subscription.dto.MatchReferralRequest;
import fr.nbank.subscription.dto.ReferralResponse;
import fr.nbank.subscription.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/referrals")
@Tag(name = "Parrainage", description = "Invite friends: save email, track status, credit after validation")
public class ReferralController {
  private final ReferralService service;
  private final boolean authDisabled;
  private final String internalKey;

  public ReferralController(
      ReferralService service,
      @Value("${app.security.disabled:false}") boolean authDisabled,
      @Value("${app.internal.api-key:}") String internalKey) {
    this.service = service;
    this.authDisabled = authDisabled;
    this.internalKey = internalKey == null ? "" : internalKey;
  }

  @PostMapping
  @Operation(summary = "Invite a friend by email (parrainage)")
  public ResponseEntity<ReferralResponse> invite(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateReferralRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(ownerId(jwt), request));
  }

  @GetMapping
  @Operation(summary = "List your parrainage invitations and status")
  public List<ReferralResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(ownerId(jwt));
  }

  @PostMapping("/internal/match")
  @Operation(summary = "Internal: match invited email after account creation")
  public ReferralResponse match(
      @RequestHeader(value = "X-Internal-Key", required = false) String key,
      @Valid @RequestBody MatchReferralRequest request) {
    assertInternal(key);
    return service.match(request);
  }

  @PostMapping("/internal/{id}/workflow")
  @Operation(summary = "Internal: attach Ballerine workflow id")
  public ReferralResponse attachWorkflow(
      @RequestHeader(value = "X-Internal-Key", required = false) String key,
      @PathVariable UUID id,
      @RequestBody Map<String, String> body) {
    assertInternal(key);
    String workflowRuntimeId = body != null ? body.get("workflowRuntimeId") : null;
    if (workflowRuntimeId == null || workflowRuntimeId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workflowRuntimeId is required");
    }
    return service.attachWorkflow(id, workflowRuntimeId);
  }

  @PostMapping("/internal/workflow/{workflowRuntimeId}/approve")
  @Operation(summary = "Internal: agent approved Ballerine referral workflow")
  public ReferralResponse approve(
      @RequestHeader(value = "X-Internal-Key", required = false) String key,
      @PathVariable String workflowRuntimeId) {
    assertInternal(key);
    return service.approveByWorkflow(workflowRuntimeId);
  }

  @PostMapping("/internal/workflow/{workflowRuntimeId}/reject")
  @Operation(summary = "Internal: agent rejected Ballerine referral workflow")
  public ReferralResponse reject(
      @RequestHeader(value = "X-Internal-Key", required = false) String key,
      @PathVariable String workflowRuntimeId) {
    assertInternal(key);
    return service.rejectByWorkflow(workflowRuntimeId);
  }

  @PostMapping("/internal/{id}/credited")
  @Operation(summary = "Internal: mark reward credited on referrer account")
  public ReferralResponse credited(
      @RequestHeader(value = "X-Internal-Key", required = false) String key, @PathVariable UUID id) {
    assertInternal(key);
    return service.markCredited(id);
  }

  private void assertInternal(String key) {
    if (authDisabled) return;
    if (internalKey.isBlank() || key == null || !internalKey.equals(key)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal key required");
    }
  }

  private String ownerId(Jwt jwt) {
    if (jwt != null && !jwt.getSubject().isBlank()) return jwt.getSubject();
    if (authDisabled) return "local-demo";
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
  }
}
