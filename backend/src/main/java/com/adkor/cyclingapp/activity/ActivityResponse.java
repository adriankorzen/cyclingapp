package com.adkor.cyclingapp.activity;

import java.time.Instant;
import java.time.LocalDateTime;

public record ActivityResponse(
		Long id,
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

	static ActivityResponse from(Activity activity) {
		return new ActivityResponse(
				activity.getId(),
				activity.getSource(),
				activity.getExternalId(),
				activity.getName(),
				activity.getSportType(),
				activity.getStartTime(),
				activity.getStartTimeLocal(),
				activity.getDurationSeconds(),
				activity.getMovingDurationSeconds(),
				activity.getDistanceMeters(),
				activity.getElevationGainMeters(),
				activity.getAverageHeartRate(),
				activity.getMaxHeartRate(),
				activity.getAveragePower(),
				activity.getMaxPower(),
				activity.getNormalizedPower(),
				activity.getAverageCadence(),
				activity.getCalories());
	}

}
