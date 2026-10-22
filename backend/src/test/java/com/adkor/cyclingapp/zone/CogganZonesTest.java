package com.adkor.cyclingapp.zone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

class CogganZonesTest {

	@Test
	void calculatesSevenPowerZonesFromFtp() {
		List<TrainingZone> zones = CogganZones.powerZones(250);

		assertThat(zones).containsExactly(
				new TrainingZone(1, "Active Recovery", 0, 55, 0, 138),
				new TrainingZone(2, "Endurance", 56, 75, 139, 188),
				new TrainingZone(3, "Tempo", 76, 90, 189, 225),
				new TrainingZone(4, "Lactate Threshold", 91, 105, 226, 263),
				new TrainingZone(5, "VO2 Max", 106, 120, 264, 300),
				new TrainingZone(6, "Anaerobic Capacity", 121, 150, 301, 375),
				new TrainingZone(7, "Neuromuscular Power", 151, null, 376, null));
	}

	@Test
	void calculatesFiveHeartRateZonesFromLthr() {
		List<TrainingZone> zones = CogganZones.heartRateZones(170);

		assertThat(zones).containsExactly(
				new TrainingZone(1, "Active Recovery", 0, 68, 0, 116),
				new TrainingZone(2, "Endurance", 69, 83, 117, 141),
				new TrainingZone(3, "Tempo", 84, 94, 142, 160),
				new TrainingZone(4, "Lactate Threshold", 95, 105, 161, 179),
				new TrainingZone(5, "VO2 Max", 106, null, 180, null));
	}

	@Test
	void zonesAreContiguous() {
		List<TrainingZone> zones = CogganZones.powerZones(287);

		for (int i = 1; i < zones.size(); i++) {
			assertThat(zones.get(i).min()).isEqualTo(zones.get(i - 1).max() + 1);
		}
	}

	@Test
	void rejectsNonPositiveThreshold() {
		assertThatIllegalArgumentException().isThrownBy(() -> CogganZones.powerZones(0));
		assertThatIllegalArgumentException().isThrownBy(() -> CogganZones.heartRateZones(-1));
	}

}
