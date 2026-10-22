package com.adkor.cyclingapp.zone;

/**
 * A single training zone with bounds expressed both as a percentage of the threshold value
 * and as absolute values (watts or bpm). Upper bounds are {@code null} for open-ended zones.
 */
public record TrainingZone(
		int number,
		String name,
		int minPercent,
		Integer maxPercent,
		int min,
		Integer max) {
}
