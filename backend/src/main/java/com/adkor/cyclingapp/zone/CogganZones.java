package com.adkor.cyclingapp.zone;

import java.util.ArrayList;
import java.util.List;

/**
 * Training zones according to Andrew Coggan's model: power zones based on FTP
 * and heart rate zones based on LTHR.
 */
public final class CogganZones {

	private record ZoneDefinition(String name, Integer maxPercent) {
	}

	private static final List<ZoneDefinition> POWER_ZONES = List.of(
			new ZoneDefinition("Active Recovery", 55),
			new ZoneDefinition("Endurance", 75),
			new ZoneDefinition("Tempo", 90),
			new ZoneDefinition("Lactate Threshold", 105),
			new ZoneDefinition("VO2 Max", 120),
			new ZoneDefinition("Anaerobic Capacity", 150),
			new ZoneDefinition("Neuromuscular Power", null));

	private static final List<ZoneDefinition> HEART_RATE_ZONES = List.of(
			new ZoneDefinition("Active Recovery", 68),
			new ZoneDefinition("Endurance", 83),
			new ZoneDefinition("Tempo", 94),
			new ZoneDefinition("Lactate Threshold", 105),
			new ZoneDefinition("VO2 Max", null));

	private CogganZones() {
	}

	public static List<TrainingZone> powerZones(int ftpWatts) {
		return calculate(ftpWatts, POWER_ZONES);
	}

	public static List<TrainingZone> heartRateZones(int lthrBpm) {
		return calculate(lthrBpm, HEART_RATE_ZONES);
	}

	private static List<TrainingZone> calculate(int threshold, List<ZoneDefinition> definitions) {
		if (threshold <= 0) {
			throw new IllegalArgumentException("Threshold must be positive, got " + threshold);
		}
		List<TrainingZone> zones = new ArrayList<>(definitions.size());
		// Each zone starts right above the previous one, so the zones are contiguous and never overlap
		int minPercent = 0;
		int min = 0;
		for (int i = 0; i < definitions.size(); i++) {
			ZoneDefinition definition = definitions.get(i);
			Integer maxPercent = definition.maxPercent();
			Integer max = maxPercent == null ? null : percentOf(threshold, maxPercent);
			zones.add(new TrainingZone(i + 1, definition.name(), minPercent, maxPercent, min, max));
			if (max != null) {
				minPercent = maxPercent + 1;
				min = max + 1;
			}
		}
		return List.copyOf(zones);
	}

	private static int percentOf(int value, int percent) {
		return (int) Math.round(value * percent / 100.0);
	}

}
