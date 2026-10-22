package com.adkor.cyclingapp.rider;

import java.time.Instant;
import java.time.LocalDate;

public record ThresholdResponse(
		Long id,
		LocalDate effectiveFrom,
		Integer ftpWatts,
		Integer lthrBpm,
		Integer maxHeartRateBpm,
		Instant createdAt) {

	static ThresholdResponse from(RiderThreshold threshold) {
		return new ThresholdResponse(
				threshold.getId(),
				threshold.getEffectiveFrom(),
				threshold.getFtpWatts(),
				threshold.getLthrBpm(),
				threshold.getMaxHeartRateBpm(),
				threshold.getCreatedAt());
	}

}
