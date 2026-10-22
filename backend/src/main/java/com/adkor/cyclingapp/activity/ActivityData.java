package com.adkor.cyclingapp.activity;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Source-independent activity summary used to import activities from external systems.
 */
public record ActivityData(
		ActivitySource source,
		String externalId,
		String name,
		String sportType,
		Instant startTime,
		LocalDateTime startTimeLocal,
		Integer durationSeconds,
		Integer movingDurationSeconds,
		Double distanceMeters,
		Double elevationGainMeters,
		Integer averageHeartRate,
		Integer maxHeartRate,
		Integer averagePower,
		Integer maxPower,
		Integer normalizedPower,
		Integer averageCadence,
		Integer calories) {
}
