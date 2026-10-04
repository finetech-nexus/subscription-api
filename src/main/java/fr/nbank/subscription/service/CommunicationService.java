package fr.nbank.subscription.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sends transactional email/SMS via Novu — same communication platform as
 * {@code iam-customs-authenticators} (authenticator / registration OTP email).
 */
@Service
public class CommunicationService {
  private static final Logger log = LoggerFactory.getLogger(CommunicationService.class);
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private final String triggerUrl;
  private final String apiKey;
  private final String parrainageEmailWorkflowId;

  public CommunicationService(
      @Value("${app.novu.trigger-url:https://api.novu.co/v1/events/trigger}") String triggerUrl,
      @Value("${app.novu.api-key:}") String apiKey,
      @Value("${app.novu.parrainage-email-workflow-id:parrainage-email-workflow}") String parrainageEmailWorkflowId) {
    this.triggerUrl = triggerUrl == null ? "" : triggerUrl.trim();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.parrainageEmailWorkflowId =
        parrainageEmailWorkflowId == null ? "" : parrainageEmailWorkflowId.trim();
  }

  public void sendParrainageInvite(
      String invitedEmail,
      BigDecimal rewardAmount,
      String currency,
      String campaignCode,
      String referrerLabel) {
    if (!configured()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Communication platform is not configured (NOVU_API_KEY / PARRAINAGE_EMAIL_WORKFLOW_ID)");
    }
    String email = invitedEmail == null ? "" : invitedEmail.trim().toLowerCase();
    if (email.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invitedEmail is required");
    }
    String amount = rewardAmount == null ? "0" : rewardAmount.stripTrailingZeros().toPlainString();
    String cur = currency == null || currency.isBlank() ? "EUR" : currency.trim().toUpperCase();
    String campaign = campaignCode == null || campaignCode.isBlank() ? "PARRAINAGE" : campaignCode.trim();
    String referrer = referrerLabel == null || referrerLabel.isBlank() ? "Un ami" : referrerLabel.trim();

    log.info(
        "Sending parrainage email via Novu to {} workflowId={}",
        maskRecipient(email),
        parrainageEmailWorkflowId);

    String body = "{"
        + "\"name\":\"" + escapeJson(parrainageEmailWorkflowId) + "\","
        + "\"to\":{"
        + "\"subscriberId\":\"" + escapeJson(email) + "\","
        + "\"email\":\"" + escapeJson(email) + "\""
        + "},"
        + "\"payload\":{"
        + "\"payload\":{"
        + "\"invitedEmail\":\"" + escapeJson(email) + "\","
        + "\"rewardAmount\":\"" + escapeJson(amount) + "\","
        + "\"rewardCurrency\":\"" + escapeJson(cur) + "\","
        + "\"campaignCode\":\"" + escapeJson(campaign) + "\","
        + "\"referrer\":\"" + escapeJson(referrer) + "\","
        + "\"message\":\"" + escapeJson(
            "Votre ami " + referrer + " vous invite sur Nexus Bank. Inscrivez-vous pour profiter de l'offre de parrainage.")
        + "\""
        + "}"
        + "}"
        + "}";

    trigger(body, email);
  }

  private void trigger(String requestBody, String recipient) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(triggerUrl))
        .timeout(Duration.ofSeconds(20))
        .header("Authorization", "ApiKey " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
    try {
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.error(
            "Novu parrainage email failed for {} status={} body={}",
            maskRecipient(recipient),
            response.statusCode(),
            response.body());
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Failed to send invitation email via communication platform");
      }
      log.info(
          "Novu parrainage email succeeded for {} response={}",
          maskRecipient(recipient),
          response.body());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      log.error("I/O failure sending Novu parrainage email to {}", maskRecipient(recipient), e);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Unable to reach communication platform", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Communication platform call interrupted", e);
    }
  }

  private boolean configured() {
    return !apiKey.isBlank() && !triggerUrl.isBlank() && !parrainageEmailWorkflowId.isBlank();
  }

  private static String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }

  private static String maskRecipient(String recipient) {
    if (recipient == null || recipient.length() <= 4) return "****";
    return "****" + recipient.substring(recipient.length() - 4);
  }
}
