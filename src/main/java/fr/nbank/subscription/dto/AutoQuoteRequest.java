package fr.nbank.subscription.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AutoQuoteRequest(
    @NotBlank @Size(max = 16) String registrationNumber,
    @NotBlank @Size(max = 60) String make,
    @NotBlank @Size(max = 60) String model,
    @NotNull @Min(1980) Integer firstRegistrationYear,
    @NotNull @Min(1) @Max(40) Integer horsepower,
    @NotBlank @Size(max = 120) String insuredName,
    @NotBlank @Size(max = 20) String nationalId,
    @NotBlank @Pattern(regexp = "^[0-9]{8}$") String phone,
    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") @Size(max = 120) String email,
    @NotBlank @Pattern(regexp = "^(third_party|comprehensive)$") String coverage,
    @NotNull LocalDate startDate
) {}
