package com.adkor.cyclingapp.rider;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Rider profile data. Training thresholds are managed separately as a history
 * ({@code /api/riders/{id}/thresholds}).
 */
public record RiderRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @DecimalMin("20.0") @DecimalMax("250.0") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
		@Min(25) @Max(120) Integer restingHeartRateBpm) {
}
