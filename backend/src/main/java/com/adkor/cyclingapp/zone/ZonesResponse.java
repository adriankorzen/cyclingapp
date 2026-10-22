package com.adkor.cyclingapp.zone;

import java.util.List;

public record ZonesResponse(
		Integer ftpWatts,
		Integer lthrBpm,
		List<TrainingZone> powerZones,
		List<TrainingZone> heartRateZones) {
}
