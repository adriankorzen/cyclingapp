package com.adkor.cyclingapp.analysis;

import java.util.List;
import java.util.stream.IntStream;

import com.adkor.cyclingapp.zone.TrainingZone;

/**
 * Time spent in a training zone.
 */
public record ZoneTime(int number, String name, int seconds, double percent) {

	/**
	 * Counts samples (seconds) per zone. A value belongs to a zone when it is at least the zone's
	 * minimum and below the next zone's minimum, so the result matches the zone bounds shown to the rider.
	 */
	static List<ZoneTime> distribution(double[] values, List<TrainingZone> zones) {
		int[] seconds = new int[zones.size()];
		for (double value : values) {
			if (Double.isNaN(value)) {
				continue;
			}
			for (int z = 0; z < zones.size(); z++) {
				Integer max = zones.get(z).max();
				if (value >= zones.get(z).min() && (max == null || value < max + 1)) {
					seconds[z]++;
					break;
				}
			}
		}
		int n = values.length;
		return IntStream.range(0, zones.size())
				.mapToObj(z -> new ZoneTime(zones.get(z).number(), zones.get(z).name(), seconds[z],
						n == 0 ? 0.0 : seconds[z] * 100.0 / n))
				.toList();
	}

}
