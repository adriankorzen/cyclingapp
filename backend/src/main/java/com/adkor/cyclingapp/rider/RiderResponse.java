package com.adkor.cyclingapp.rider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record RiderResponse(
		Long id,
		String name,
		BigDecimal weightKg,
		Integer ftpWatts,
		BigDecimal ftpWattsPerKg,
		Integer lthrBpm,
		Integer maxHeartRateBpm,
		Integer restingHeartRateBpm,
		Instant createdAt,
		Instant updatedAt) {

	static RiderResponse from(Rider rider) {
		return new RiderResponse(
				rider.getId(),
				rider.getName(),
				rider.getWeightKg(),
				rider.getFtpWatts(),
				wattsPerKg(rider.getFtpWatts(), rider.getWeightKg()),
				rider.getLthrBpm(),
				rider.getMaxHeartRateBpm(),
				rider.getRestingHeartRateBpm(),
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
