package fr.nbank.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.nbank.subscription.dto.AutoQuoteRequest;
import fr.nbank.subscription.dto.AutoQuoteResponse;
import fr.nbank.subscription.dto.QuoteOption;
import fr.nbank.subscription.entity.AutoQuote;
import fr.nbank.subscription.repository.AutoQuoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutoQuoteService {
  private final AutoQuoteRepository quotes;
  private final ObjectMapper objectMapper;

  public AutoQuoteService(AutoQuoteRepository quotes, ObjectMapper objectMapper) {
    this.quotes=quotes; this.objectMapper=objectMapper;
  }

  @Transactional
  public AutoQuoteResponse create(String ownerId, AutoQuoteRequest request) {
    if (request.firstRegistrationYear() > LocalDate.now().getYear()) {
      throw new IllegalArgumentException("First registration year cannot be in the future");
    }
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Instant expiresAt = now.plus(24, ChronoUnit.HOURS);
    String vehicle = request.make().trim() + " " + request.model().trim();
    List<QuoteOption> options = quoteOptions(request);
    try {
      quotes.save(new AutoQuote(id, ownerId, request.registrationNumber().trim().toUpperCase(), vehicle,
          request.insuredName().trim(), objectMapper.writeValueAsString(request), objectMapper.writeValueAsString(options),
          "AVAILABLE", now, expiresAt));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not store the quotation", e);
    }
    return response(id, "AVAILABLE", expiresAt, request.registrationNumber().trim().toUpperCase(), vehicle, request.insuredName().trim(), options);
  }

  @Transactional(readOnly = true)
  public AutoQuoteResponse get(String ownerId, UUID id) {
    AutoQuote quote = quotes.findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation not found"));
    if (quote.getExpiresAt().isBefore(Instant.now()) && "AVAILABLE".equals(quote.getStatus())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Quotation has expired");
    }
    return response(quote.getId(), quote.getStatus(), quote.getExpiresAt(), quote.getRegistrationNumber(),
        quote.getVehicleDescription(), quote.getInsuredName(), readOptions(quote.getOptionsJson()));
  }

  @Transactional(readOnly = true)
  public AutoQuote requireAvailable(String ownerId, UUID id) {
    AutoQuote quote = quotes.findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation not found"));
    if (!"AVAILABLE".equals(quote.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Quotation is already used");
    if (quote.getExpiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Quotation has expired");
    return quote;
  }

  @Transactional
  public void markConverted(AutoQuote quote) {
    quote.setStatus("CONVERTED");
    quotes.save(quote);
  }

  public AutoQuoteRequest readRequest(String json) {
    try { return objectMapper.readValue(json, AutoQuoteRequest.class); }
    catch (JsonProcessingException e) { throw new IllegalStateException("Quotation details are unreadable", e); }
  }

  public List<QuoteOption> readOptions(String json) {
    try { return objectMapper.readValue(json, new TypeReference<List<QuoteOption>>() {}); }
    catch (JsonProcessingException e) { throw new IllegalStateException("Quotation plans are unreadable", e); }
  }

  private List<QuoteOption> quoteOptions(AutoQuoteRequest request) {
    int age = Math.max(0, LocalDate.now().getYear() - request.firstRegistrationYear());
    BigDecimal vehicleBase = BigDecimal.valueOf(165L + request.horsepower() * 23L)
        .multiply(BigDecimal.valueOf(1d + Math.min(age, 30) * 0.012d));
    BigDecimal liability = roundToFive(vehicleBase);
    List<QuoteOption> options = new ArrayList<>();
    options.add(new QuoteOption("ESSENTIELLE", "Nexus Essential", "Third-party cover · annual estimate",
        liability, liability.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP), "TND",
        List.of("Mandatory third-party liability", "Basic roadside assistance")));
    if ("comprehensive".equals(request.coverage())) {
      BigDecimal comfort = roundToFive(vehicleBase.multiply(BigDecimal.valueOf(1.75)));
      BigDecimal allRisk = roundToFive(vehicleBase.multiply(BigDecimal.valueOf(2.55)));
      options.add(new QuoteOption("CONFORT", "Nexus Comfort", "Extended protection · annual estimate",
          comfort, comfort.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP), "TND",
          List.of("Third-party liability", "Glass breakage", "Extended roadside assistance")));
      options.add(new QuoteOption("TOUS_RISQUES", "Nexus All-risk", "Maximum protection · annual estimate",
          allRisk, allRisk.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP), "TND",
          List.of("Comfort coverages", "Accidental damage", "Theft and fire")));
    }
    return options;
  }

  private static BigDecimal roundToFive(BigDecimal amount) {
    return amount.divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(5));
  }

  private static AutoQuoteResponse response(UUID id, String status, Instant expiresAt, String registration, String vehicle,
      String insuredName, List<QuoteOption> options) {
    return new AutoQuoteResponse(id, status, true, expiresAt, registration, vehicle, insuredName, options);
  }
}
