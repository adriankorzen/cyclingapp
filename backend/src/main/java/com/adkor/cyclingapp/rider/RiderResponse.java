package com.adkor.cyclingapp.rider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * @param currentThresholds thresholds valid today, {@code null} when the rider has none yet
 * @param ftpWattsPerKg current FTP relative to body weight
 */
public record RiderResponse(
		Long id,
		String name,
		BigDecimal weightKg,
		Integer restingHeartRateBpm,
		ThresholdResponse currentThresholds,
		BigDecimal ftpWattsPerKg,
		Instant createdAt,
		Instant updatedAt) {

	static RiderResponse from(Rider rider, RiderThreshold currentThresholds) {
		Integer ftp = currentThresholds != null ? currentThresholds.getFtpWatts() : null;
		return new RiderResponse(
				rider.getId(),
				rider.getName(),
				rider.getWeightKg(),
				rider.getRestingHeartRateBpm(),
				currentThresholds != null ? ThresholdResponse.from(currentThresholds) : null,
				wattsPerKg(ftp, rider.getWeightKg()),
				rider.getCreatedAt(),
				rider.getUpdatedAt());
	}

	private static BigDecimal wattsPerKg(Integer watts, BigDecimal weightKg) {
		if (watts == null) {
			return null;
		}
		return BigDecimal.valueOf(watts).divide(weightKg, 2, RoundingMode.HALF_UP);
	}

}
