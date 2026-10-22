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

public record RiderRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @DecimalMin("20.0") @DecimalMax("250.0") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
		@Min(30) @Max(2500) Integer ftpWatts,
		@Min(60) @Max(230) Integer lthrBpm,
		@Min(80) @Max(240) Integer maxHeartRateBpm,
		@Min(25) @Max(120) Integer restingHeartRateBpm) {
}
