package com.adkor.cyclingapp.zone;

import java.time.LocalDate;
import java.util.List;

/**
 * @param thresholdsEffectiveFrom start date of the threshold entry the zones are calculated from
 */
public record ZonesResponse(
		LocalDate thresholdsEffectiveFrom,
		Integer ftpWatts,
		Integer lthrBpm,
		List<TrainingZone> powerZones,
		List<TrainingZone> heartRateZones) {
}
