package com.adkor.cyclingapp.garmin;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Activity summary as returned by the Garmin activity search endpoint (only the fields we use).
 * Durations are in seconds, distances and elevation in meters.
 */
public record GarminActivity(
		long activityId,
		String activityName,
		ActivityType activityType,
		@JsonFormat(pattern = GARMIN_DATE_TIME) LocalDateTime startTimeGMT,
		@JsonFormat(pattern = GARMIN_DATE_TIME) LocalDateTime startTimeLocal,
		Double duration,
		Double movingDuration,
		Double distance,
		Double elevationGain,
		Double averageHR,
		Double maxHR,
		Double avgPower,
		Double maxPower,
		Double normPower,
		Double averageBikingCadenceInRevPerMinute,
		Double calories) {

	static final String GARMIN_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

	public record ActivityType(String typeKey) {
	}

}
